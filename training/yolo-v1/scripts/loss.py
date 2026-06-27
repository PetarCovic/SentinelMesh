import torch
import torch.nn as nn


class YoloV1Loss(nn.Module):
    def __init__(
        self,
        grid_size: int,
        boxes_per_cell: int,
        class_count: int,
        lambda_coord: float,
        lambda_object: float,
        lambda_no_object: float
    ):
        super().__init__()

        self.grid_size = grid_size
        self.boxes_per_cell = boxes_per_cell
        self.class_count = class_count
        self.lambda_coord = lambda_coord
        self.lambda_object = lambda_object
        self.lambda_no_object = lambda_no_object

    def forward(self, predictions: torch.Tensor, targets: torch.Tensor) -> torch.Tensor:
        class_pred = predictions[..., :self.class_count]
        class_target = targets[..., :self.class_count]

        total_coord_loss = torch.tensor(0.0, device=predictions.device)
        total_object_loss = torch.tensor(0.0, device=predictions.device)
        total_no_object_loss = torch.tensor(0.0, device=predictions.device)

        object_cell_mask = self._get_object_cell_mask(targets)

        if object_cell_mask.any():
            class_loss = torch.sum((class_pred[object_cell_mask] - class_target[object_cell_mask]) ** 2)
        else:
            class_loss = torch.tensor(0.0, device=predictions.device)

        for box_index in range(self.boxes_per_cell):
            box_start = self.class_count + box_index * 5

            pred_x = predictions[..., box_start]
            pred_y = predictions[..., box_start + 1]
            pred_w = predictions[..., box_start + 2]
            pred_h = predictions[..., box_start + 3]
            pred_obj = predictions[..., box_start + 4]

            target_x = targets[..., box_start]
            target_y = targets[..., box_start + 1]
            target_w = targets[..., box_start + 2]
            target_h = targets[..., box_start + 3]
            target_obj = targets[..., box_start + 4]

            object_mask = target_obj > 0.0
            no_object_mask = target_obj == 0.0

            if object_mask.any():
                coord_loss = torch.sum((pred_x[object_mask] - target_x[object_mask]) ** 2)
                coord_loss += torch.sum((pred_y[object_mask] - target_y[object_mask]) ** 2)

                pred_w_safe = torch.clamp(pred_w[object_mask], min=1e-6)
                pred_h_safe = torch.clamp(pred_h[object_mask], min=1e-6)

                target_w_safe = torch.clamp(target_w[object_mask], min=1e-6)
                target_h_safe = torch.clamp(target_h[object_mask], min=1e-6)

                coord_loss += torch.sum((torch.sqrt(pred_w_safe) - torch.sqrt(target_w_safe)) ** 2)
                coord_loss += torch.sum((torch.sqrt(pred_h_safe) - torch.sqrt(target_h_safe)) ** 2)

                object_loss = torch.sum((pred_obj[object_mask] - target_obj[object_mask]) ** 2)
            else:
                coord_loss = torch.tensor(0.0, device=predictions.device)
                object_loss = torch.tensor(0.0, device=predictions.device)

            if no_object_mask.any():
                no_object_loss = torch.sum((pred_obj[no_object_mask] - target_obj[no_object_mask]) ** 2)
            else:
                no_object_loss = torch.tensor(0.0, device=predictions.device)

            total_coord_loss += coord_loss
            total_object_loss += object_loss
            total_no_object_loss += no_object_loss

        total_loss = (
            self.lambda_coord * total_coord_loss
            + self.lambda_object * total_object_loss
            + self.lambda_no_object * total_no_object_loss
            + class_loss
        )

        batch_size = predictions.shape[0]
        return total_loss / batch_size

    def _get_object_cell_mask(self, targets: torch.Tensor) -> torch.Tensor:
        object_cell_mask = torch.zeros(
            targets.shape[0],
            targets.shape[1],
            targets.shape[2],
            dtype=torch.bool,
            device=targets.device
        )

        for box_index in range(self.boxes_per_cell):
            box_start = self.class_count + box_index * 5
            target_obj = targets[..., box_start + 4]
            object_cell_mask = object_cell_mask | (target_obj > 0.0)

        return object_cell_mask