import argparse
import json
import os
import struct
import sys

import torch

from model import YoloV1PersonSmall


HEADER = "SMYOLOV1"
VERSION = 2


def write_java_utf(file, value):
    encoded = value.encode("utf-8")

    if len(encoded) > 65535:
        raise ValueError(f"String too long for Java writeUTF format: {value}")

    file.write(struct.pack(">H", len(encoded)))
    file.write(encoded)


def write_int(file, value):
    file.write(struct.pack(">i", int(value)))


def write_boolean(file, value):
    file.write(struct.pack(">?", bool(value)))


def write_float(file, value):
    file.write(struct.pack(">f", float(value)))


def write_1d(file, values):
    write_int(file, values.shape[0])

    for i in range(values.shape[0]):
        write_float(file, values[i])


def write_2d(file, values):
    write_int(file, values.shape[0])
    write_int(file, values.shape[1])

    for i in range(values.shape[0]):
        for j in range(values.shape[1]):
            write_float(file, values[i][j])


def write_4d(file, values):
    write_int(file, values.shape[0])
    write_int(file, values.shape[1])
    write_int(file, values.shape[2])
    write_int(file, values.shape[3])

    for a in range(values.shape[0]):
        for b in range(values.shape[1]):
            for c in range(values.shape[2]):
                for d in range(values.shape[3]):
                    write_float(file, values[a][b][c][d])


def export_conv_layer(
        file,
        conv_layer,
        stride,
        padding,
        apply_leaky_relu_after,
        apply_max_pool_after,
        max_pool_size,
        max_pool_stride):
    """
    PyTorch Conv2d weight shape:
        [out_channels][in_channels][kernel_y][kernel_x]

    Java ConvolutionLayer filter shape:
        [filter_index][kernel_y][kernel_x][input_channel]
    """

    filters = conv_layer.weight.detach().cpu().numpy()
    filters = filters.transpose(0, 2, 3, 1)

    biases = conv_layer.bias.detach().cpu().numpy()

    write_int(file, stride)
    write_java_utf(file, padding)
    write_boolean(file, apply_leaky_relu_after)
    write_boolean(file, apply_max_pool_after)
    write_int(file, max_pool_size)
    write_int(file, max_pool_stride)

    write_4d(file, filters)
    write_1d(file, biases)


def load_config(config_path):
    with open(config_path, "r", encoding="utf-8") as file:
        return json.load(file)


def load_checkpoint(checkpoint_path, model, device):
    checkpoint = torch.load(checkpoint_path, map_location=device)

    if isinstance(checkpoint, dict) and "model_state_dict" in checkpoint:
        model.load_state_dict(checkpoint["model_state_dict"])
    elif isinstance(checkpoint, dict) and "state_dict" in checkpoint:
        model.load_state_dict(checkpoint["state_dict"])
    else:
        model.load_state_dict(checkpoint)

    return model


def validate_model_architecture(model):
    if len(model.features) != 6:
        raise ValueError(
            "Expected model.features to contain 6 layers: "
            "Conv1, LeakyReLU, MaxPool, Conv2, LeakyReLU, MaxPool"
        )

    conv1 = model.features[0]
    conv2 = model.features[3]

    if not isinstance(conv1, torch.nn.Conv2d):
        raise TypeError("Expected model.features[0] to be Conv2d")

    if not isinstance(conv2, torch.nn.Conv2d):
        raise TypeError("Expected model.features[3] to be Conv2d")

    if conv1.in_channels != 3 or conv1.out_channels != 16:
        raise ValueError("Conv1 must be Conv2d(3, 16, kernel_size=7, stride=2, padding=3)")

    if conv1.kernel_size != (7, 7) or conv1.stride != (2, 2) or conv1.padding != (3, 3):
        raise ValueError("Conv1 has incorrect kernel/stride/padding")

    if conv2.in_channels != 16 or conv2.out_channels != 32:
        raise ValueError("Conv2 must be Conv2d(16, 32, kernel_size=3, stride=1, padding=1)")

    if conv2.kernel_size != (3, 3) or conv2.stride != (1, 1) or conv2.padding != (1, 1):
        raise ValueError("Conv2 has incorrect kernel/stride/padding")

    if model.hidden_fc.in_features != 56 * 56 * 32:
        raise ValueError(f"hidden_fc input size should be {56 * 56 * 32}")

    if model.hidden_fc.out_features != 128:
        raise ValueError("hidden_fc output size should be 128")

    if model.output_fc.in_features != 128:
        raise ValueError("output_fc input size should be 128")


def export_weights(config_path, checkpoint_path, export_path):
    config = load_config(config_path)

    grid_size = int(config.get("gridSize", 7))
    boxes_per_cell = int(config.get("boxesPerCell", 2))
    class_names = config.get("classNames", ["person"])
    class_count = len(class_names)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    model = YoloV1PersonSmall(
        grid_size=grid_size,
        boxes_per_cell=boxes_per_cell,
        class_count=class_count
    ).to(device)

    model = load_checkpoint(checkpoint_path, model, device)
    model.eval()

    validate_model_architecture(model)

    parent_dir = os.path.dirname(export_path)

    if parent_dir:
        os.makedirs(parent_dir, exist_ok=True)

    conv1 = model.features[0]
    conv2 = model.features[3]

    hidden_weights = model.hidden_fc.weight.detach().cpu().numpy()
    hidden_biases = model.hidden_fc.bias.detach().cpu().numpy()

    output_weights = model.output_fc.weight.detach().cpu().numpy()
    output_biases = model.output_fc.bias.detach().cpu().numpy()

    with open(export_path, "wb") as file:
        write_java_utf(file, HEADER)
        write_int(file, VERSION)

        write_int(file, 2)

        export_conv_layer(
            file=file,
            conv_layer=conv1,
            stride=2,
            padding="SAME",
            apply_leaky_relu_after=True,
            apply_max_pool_after=True,
            max_pool_size=2,
            max_pool_stride=2
        )

        export_conv_layer(
            file=file,
            conv_layer=conv2,
            stride=1,
            padding="SAME",
            apply_leaky_relu_after=True,
            apply_max_pool_after=True,
            max_pool_size=2,
            max_pool_stride=2
        )

        write_2d(file, hidden_weights)
        write_1d(file, hidden_biases)

        write_2d(file, output_weights)
        write_1d(file, output_biases)

    print("Exported YOLOv1 weights")
    print(f"Config: {config_path}")
    print(f"Checkpoint: {checkpoint_path}")
    print(f"Export path: {export_path}")
    print(f"Format version: {VERSION}")
    print("Conv layers exported: 2")
    print(f"Hidden weights shape: {hidden_weights.shape}")
    print(f"Output weights shape: {output_weights.shape}")


def parse_args():
    parser = argparse.ArgumentParser(description="Export YOLOv1 PyTorch checkpoint to SentinelMesh Java weights format.")

    parser.add_argument(
        "--config",
        required=True,
        help="Path to YOLOv1 training config JSON."
    )

    parser.add_argument(
        "--checkpoint",
        required=True,
        help="Path to PyTorch checkpoint, usually training/yolo-v1/checkpoints/best.pt."
    )

    parser.add_argument(
        "--export",
        required=True,
        help="Output path for Java .weights file."
    )

    return parser.parse_args()


def main():
    args = parse_args()

    export_weights(
        config_path=args.config,
        checkpoint_path=args.checkpoint,
        export_path=args.export
    )


if __name__ == "__main__":
    main()