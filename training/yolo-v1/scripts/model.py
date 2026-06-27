import torch
import torch.nn as nn


class YoloV1PersonSmall(nn.Module):
    def __init__(self, grid_size=7, boxes_per_cell=2, class_count=1):
        super().__init__()

        self.grid_size = grid_size
        self.boxes_per_cell = boxes_per_cell
        self.class_count = class_count
        self.values_per_cell = class_count + boxes_per_cell * 5
        self.output_size = grid_size * grid_size * self.values_per_cell

        self.features = nn.Sequential(
            nn.Conv2d(3, 16, kernel_size=7, stride=2, padding=3),
            nn.LeakyReLU(negative_slope=0.1),
            nn.MaxPool2d(kernel_size=2, stride=2),

            nn.Conv2d(16, 32, kernel_size=3, stride=1, padding=1),
            nn.LeakyReLU(negative_slope=0.1),
            nn.MaxPool2d(kernel_size=2, stride=2)
        )

        flattened_length = 56 * 56 * 32

        self.hidden_fc = nn.Linear(flattened_length, 128)
        self.hidden_activation = nn.LeakyReLU(negative_slope=0.1)
        self.output_fc = nn.Linear(128, self.output_size)

    def forward(self, x):
        x = self.features(x)
        x = torch.flatten(x, start_dim=1)
        x = self.hidden_fc(x)
        x = self.hidden_activation(x)
        x = self.output_fc(x)
        x = x.view(-1, self.grid_size, self.grid_size, self.values_per_cell)
        return x