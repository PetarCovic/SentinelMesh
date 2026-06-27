from pathlib import Path
from typing import List, Tuple

import torch
from PIL import Image
from torch.utils.data import Dataset
from torchvision.transforms import functional as F


IMAGE_EXTENSIONS = {".jpg", ".jpeg", ".png", ".bmp", ".webp"}


class YoloV1PersonDataset(Dataset):
    def __init__(
        self,
        images_dir: str,
        labels_dir: str,
        input_width: int,
        input_height: int,
        grid_size: int,
        boxes_per_cell: int,
        class_count: int
    ):
        self.images_dir = Path(images_dir)
        self.labels_dir = Path(labels_dir)
        self.input_width = input_width
        self.input_height = input_height
        self.grid_size = grid_size
        self.boxes_per_cell = boxes_per_cell
        self.class_count = class_count
        self.values_per_cell = class_count + boxes_per_cell * 5

        if not self.images_dir.exists():
            raise FileNotFoundError(f"Images directory does not exist: {self.images_dir}")

        if not self.labels_dir.exists():
            raise FileNotFoundError(f"Labels directory does not exist: {self.labels_dir}")

        self.image_paths = sorted(
            path for path in self.images_dir.iterdir()
            if path.suffix.lower() in IMAGE_EXTENSIONS
        )

        if not self.image_paths:
            raise ValueError(f"No images found in: {self.images_dir}")

    def __len__(self) -> int:
        return len(self.image_paths)

    def __getitem__(self, index: int):
        image_path = self.image_paths[index]
        label_path = self.labels_dir / f"{image_path.stem}.txt"

        image = Image.open(image_path).convert("RGB")
        image = image.resize((self.input_width, self.input_height))

        image_tensor = F.to_tensor(image)

        labels = self._load_labels(label_path)
        target = self._encode_target(labels)

        return image_tensor, target

    def _load_labels(self, label_path: Path) -> List[Tuple[int, float, float, float, float]]:
        if not label_path.exists():
            return []

        labels = []

        with label_path.open("r", encoding="utf-8") as file:
            for line_number, line in enumerate(file, start=1):
                line = line.strip()

                if not line:
                    continue

                parts = line.split()

                if len(parts) != 5:
                    raise ValueError(
                        f"Invalid label line in {label_path} line {line_number}: {line}"
                    )

                class_id = int(parts[0])
                x_center = float(parts[1])
                y_center = float(parts[2])
                width = float(parts[3])
                height = float(parts[4])

                if class_id != 0:
                    continue

                self._validate_normalized_box(
                    x_center,
                    y_center,
                    width,
                    height,
                    label_path,
                    line_number
                )

                labels.append((class_id, x_center, y_center, width, height))

        return labels

    def _validate_normalized_box(
        self,
        x_center: float,
        y_center: float,
        width: float,
        height: float,
        label_path: Path,
        line_number: int
    ):
        values = {
            "x_center": x_center,
            "y_center": y_center,
            "width": width,
            "height": height
        }

        for name, value in values.items():
            if value < 0.0 or value > 1.0:
                raise ValueError(
                    f"{name} must be between 0 and 1 in {label_path} line {line_number}"
                )

        if width <= 0.0 or height <= 0.0:
            raise ValueError(
                f"width/height must be greater than 0 in {label_path} line {line_number}"
            )

    def _encode_target(self, labels: List[Tuple[int, float, float, float, float]]) -> torch.Tensor:
        target = torch.zeros(
            self.grid_size,
            self.grid_size,
            self.values_per_cell,
            dtype=torch.float32
        )

        for class_id, x_center, y_center, width, height in labels:
            col = min(int(x_center * self.grid_size), self.grid_size - 1)
            row = min(int(y_center * self.grid_size), self.grid_size - 1)

            x_cell = x_center * self.grid_size - col
            y_cell = y_center * self.grid_size - row

            target[row, col, class_id] = 1.0

            for box_index in range(self.boxes_per_cell):
                box_start = self.class_count + box_index * 5

                target[row, col, box_start] = x_cell
                target[row, col, box_start + 1] = y_cell
                target[row, col, box_start + 2] = width
                target[row, col, box_start + 3] = height
                target[row, col, box_start + 4] = 1.0

        return target