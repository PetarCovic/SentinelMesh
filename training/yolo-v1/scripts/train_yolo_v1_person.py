import argparse
import json
from pathlib import Path

import torch
from torch.utils.data import DataLoader
from tqdm import tqdm

from dataset import YoloV1PersonDataset
from loss import YoloV1Loss
from model import YoloV1PersonSmall


def train_one_epoch(model, dataloader, loss_fn, optimizer, device):
    model.train()

    total_loss = 0.0

    progress = tqdm(dataloader, desc="Training", leave=False)

    for images, targets in progress:
        images = images.to(device)
        targets = targets.to(device)

        predictions = model(images)

        loss = loss_fn(predictions, targets)

        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        total_loss += loss.item()

        progress.set_postfix(loss=loss.item())

    return total_loss / max(len(dataloader), 1)


@torch.no_grad()
def validate(model, dataloader, loss_fn, device):
    model.eval()

    total_loss = 0.0

    progress = tqdm(dataloader, desc="Validation", leave=False)

    for images, targets in progress:
        images = images.to(device)
        targets = targets.to(device)

        predictions = model(images)
        loss = loss_fn(predictions, targets)

        total_loss += loss.item()

        progress.set_postfix(loss=loss.item())

    return total_loss / max(len(dataloader), 1)


def save_checkpoint(model, optimizer, epoch, train_loss, val_loss, checkpoint_path: Path):
    checkpoint_path.parent.mkdir(parents=True, exist_ok=True)

    torch.save(
        {
            "epoch": epoch,
            "model_state_dict": model.state_dict(),
            "optimizer_state_dict": optimizer.state_dict(),
            "train_loss": train_loss,
            "val_loss": val_loss
        },
        checkpoint_path
    )


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--config",
        default="training/yolo-v1/configs/yolo-v1-person-training-config.json"
    )
    args = parser.parse_args()

    config_path = Path(args.config)

    with config_path.open("r", encoding="utf-8") as file:
        config = json.load(file)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

    print(f"Using device: {device}")

    class_count = len(config["classNames"])

    train_dataset = YoloV1PersonDataset(
        images_dir=config["trainImagesDir"],
        labels_dir=config["trainLabelsDir"],
        input_width=config["inputWidth"],
        input_height=config["inputHeight"],
        grid_size=config["gridSize"],
        boxes_per_cell=config["boxesPerCell"],
        class_count=class_count
    )

    val_dataset = YoloV1PersonDataset(
        images_dir=config["valImagesDir"],
        labels_dir=config["valLabelsDir"],
        input_width=config["inputWidth"],
        input_height=config["inputHeight"],
        grid_size=config["gridSize"],
        boxes_per_cell=config["boxesPerCell"],
        class_count=class_count
    )

    train_loader = DataLoader(
        train_dataset,
        batch_size=config["batchSize"],
        shuffle=True,
        num_workers=0
    )

    val_loader = DataLoader(
        val_dataset,
        batch_size=config["batchSize"],
        shuffle=False,
        num_workers=0
    )

    model = YoloV1PersonSmall(
        grid_size=config["gridSize"],
        boxes_per_cell=config["boxesPerCell"],
        class_count=class_count
    ).to(device)

    loss_fn = YoloV1Loss(
        grid_size=config["gridSize"],
        boxes_per_cell=config["boxesPerCell"],
        class_count=len(config["classNames"]),
        lambda_coord=config.get("lambdaCoord", 5.0),
        lambda_object=config.get("lambdaObject", 5.0),
        lambda_no_object=config.get("lambdaNoObject", 0.05)
    )

    print("Loss weights:")
    print(f"  lambdaCoord:    {config.get('lambdaCoord', 5.0)}")
    print(f"  lambdaObject:   {config.get('lambdaObject', 5.0)}")
    print(f"  lambdaNoObject: {config.get('lambdaNoObject', 0.05)}")

    optimizer = torch.optim.Adam(
        model.parameters(),
        lr=config["learningRate"]
    )

    checkpoint_dir = Path(config["checkpointDir"])
    best_val_loss = float("inf")

    for epoch in range(1, config["epochs"] + 1):
        print(f"\nEpoch {epoch}/{config['epochs']}")

        train_loss = train_one_epoch(
            model,
            train_loader,
            loss_fn,
            optimizer,
            device
        )

        val_loss = validate(
            model,
            val_loader,
            loss_fn,
            device
        )

        print(f"Train loss: {train_loss:.6f}")
        print(f"Val loss:   {val_loss:.6f}")

        latest_checkpoint = checkpoint_dir / "latest.pt"
        save_checkpoint(
            model,
            optimizer,
            epoch,
            train_loss,
            val_loss,
            latest_checkpoint
        )

        epoch_checkpoint = checkpoint_dir / f"epoch-{epoch}.pt"

        save_checkpoint(
            model,
            optimizer,
            epoch,
            train_loss,
            val_loss,
            epoch_checkpoint
        )

        if val_loss < best_val_loss:
            best_val_loss = val_loss
            best_checkpoint = checkpoint_dir / "best.pt"

            save_checkpoint(
                model,
                optimizer,
                epoch,
                train_loss,
                val_loss,
                best_checkpoint
            )

            print(f"Saved new best checkpoint: {best_checkpoint}")


if __name__ == "__main__":
    main()