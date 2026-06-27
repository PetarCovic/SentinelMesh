import argparse
import json
import shutil
from pathlib import Path
from typing import Dict, List, Tuple


PERSON_CATEGORY_NAME = "person"


def load_json(path: Path) -> dict:
    if not path.exists():
        raise FileNotFoundError(f"Annotation file does not exist: {path}")

    with path.open("r", encoding="utf-8") as file:
        return json.load(file)


def find_person_category_id(coco: dict) -> int:
    for category in coco["categories"]:
        if category["name"] == PERSON_CATEGORY_NAME:
            return int(category["id"])

    raise ValueError("Could not find COCO category named 'person'")


def build_image_lookup(coco: dict) -> Dict[int, dict]:
    return {int(image["id"]): image for image in coco["images"]}


def collect_person_annotations(coco: dict, person_category_id: int) -> Dict[int, List[dict]]:
    annotations_by_image: Dict[int, List[dict]] = {}

    for annotation in coco["annotations"]:
        if int(annotation["category_id"]) != person_category_id:
            continue

        if annotation.get("iscrowd", 0) == 1:
            continue

        image_id = int(annotation["image_id"])
        annotations_by_image.setdefault(image_id, []).append(annotation)

    return annotations_by_image


def coco_bbox_to_yolo(
    bbox: List[float],
    image_width: int,
    image_height: int
) -> Tuple[float, float, float, float]:
    x_min, y_min, width, height = bbox

    x_center = x_min + width / 2.0
    y_center = y_min + height / 2.0

    x_center /= image_width
    y_center /= image_height
    width /= image_width
    height /= image_height

    return (
        clamp01(x_center),
        clamp01(y_center),
        clamp01(width),
        clamp01(height)
    )


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, value))


def convert_split(
    annotations_path: Path,
    source_images_dir: Path,
    output_images_dir: Path,
    output_labels_dir: Path,
    copy_images: bool,
    limit: int | None
):
    coco = load_json(annotations_path)

    person_category_id = find_person_category_id(coco)
    image_lookup = build_image_lookup(coco)
    person_annotations = collect_person_annotations(coco, person_category_id)

    output_images_dir.mkdir(parents=True, exist_ok=True)
    output_labels_dir.mkdir(parents=True, exist_ok=True)

    converted_count = 0
    skipped_missing_images = 0

    for image_id, annotations in person_annotations.items():
        if limit is not None and converted_count >= limit:
            break

        image_info = image_lookup.get(image_id)

        if image_info is None:
            continue

        file_name = image_info["file_name"]
        image_width = int(image_info["width"])
        image_height = int(image_info["height"])

        source_image_path = source_images_dir / file_name

        if not source_image_path.exists():
            skipped_missing_images += 1
            continue

        label_lines = []

        for annotation in annotations:
            x_center, y_center, width, height = coco_bbox_to_yolo(
                annotation["bbox"],
                image_width,
                image_height
            )

            if width <= 0.0 or height <= 0.0:
                continue

            # classId 0 = person
            label_lines.append(
                f"0 {x_center:.6f} {y_center:.6f} {width:.6f} {height:.6f}"
            )

        if not label_lines:
            continue

        output_label_path = output_labels_dir / f"{Path(file_name).stem}.txt"

        with output_label_path.open("w", encoding="utf-8") as file:
            file.write("\n".join(label_lines))
            file.write("\n")

        if copy_images:
            output_image_path = output_images_dir / file_name

            if not output_image_path.exists():
                shutil.copy2(source_image_path, output_image_path)

        converted_count += 1

    print(f"Converted split from: {annotations_path}")
    print(f"Converted images with person labels: {converted_count}")
    print(f"Skipped missing images: {skipped_missing_images}")
    print(f"Output images: {output_images_dir}")
    print(f"Output labels: {output_labels_dir}")


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument("--raw-root", required=True)
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--copy-images", action="store_true")
    parser.add_argument("--limit-train", type=int, default=None)
    parser.add_argument("--limit-val", type=int, default=None)

    args = parser.parse_args()

    raw_root = Path(args.raw_root)
    output_root = Path(args.output_root)

    convert_split(
        annotations_path=raw_root / "annotations" / "instances_train2017.json",
        source_images_dir=raw_root / "train2017",
        output_images_dir=output_root / "train" / "images",
        output_labels_dir=output_root / "train" / "labels",
        copy_images=args.copy_images,
        limit=args.limit_train
    )

    convert_split(
        annotations_path=raw_root / "annotations" / "instances_val2017.json",
        source_images_dir=raw_root / "val2017",
        output_images_dir=output_root / "val" / "images",
        output_labels_dir=output_root / "val" / "labels",
        copy_images=args.copy_images,
        limit=args.limit_val
    )


if __name__ == "__main__":
    main()