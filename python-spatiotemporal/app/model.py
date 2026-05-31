from typing import Dict

import torch
from torch import nn


class PositionalEncoding(nn.Module):
    def __init__(self, d_model: int, max_len: int) -> None:
        super().__init__()
        position = torch.arange(max_len).unsqueeze(1)
        div_term = torch.exp(torch.arange(0, d_model, 2) * (-torch.log(torch.tensor(10000.0)) / d_model))
        pe = torch.zeros(max_len, d_model)
        pe[:, 0::2] = torch.sin(position * div_term)
        pe[:, 1::2] = torch.cos(position * div_term)
        self.register_buffer("pe", pe.unsqueeze(0), persistent=False)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        return x + self.pe[:, :x.size(1)]


class SpatioTemporalTransformer(nn.Module):
    def __init__(
        self,
        location_vocab: int,
        time_bucket_vocab: int,
        weekday_vocab: int,
        item_type_vocab: int,
        d_model: int,
        num_heads: int,
        num_layers: int,
        ff_dim: int,
        dropout: float,
        max_seq_len: int,
    ) -> None:
        super().__init__()
        self.location_embedding = nn.Embedding(location_vocab, d_model)
        self.time_embedding = nn.Embedding(time_bucket_vocab, d_model)
        self.weekday_embedding = nn.Embedding(weekday_vocab, d_model)
        self.item_type_embedding = nn.Embedding(item_type_vocab, d_model)
        self.cls_token = nn.Parameter(torch.zeros(1, 1, d_model))
        self.position_encoding = PositionalEncoding(d_model=d_model, max_len=max_seq_len + 1)
        self.dropout = nn.Dropout(dropout)

        encoder_layer = nn.TransformerEncoderLayer(
            d_model=d_model,
            nhead=num_heads,
            dim_feedforward=ff_dim,
            dropout=dropout,
            batch_first=True,
            activation="gelu",
        )
        self.encoder = nn.TransformerEncoder(encoder_layer=encoder_layer, num_layers=num_layers)
        self.location_head = nn.Linear(d_model, location_vocab)
        self.time_head = nn.Linear(d_model, time_bucket_vocab)

    def forward(self, features: Dict[str, torch.Tensor]) -> Dict[str, torch.Tensor]:
        location_ids = features["location_ids"]
        time_buckets = features["time_buckets"]
        weekdays = features["weekdays"]
        item_type_ids = features["item_type_ids"]
        padding_mask = features["padding_mask"]

        x = (
            self.location_embedding(location_ids)
            + self.time_embedding(time_buckets)
            + self.weekday_embedding(weekdays)
            + self.item_type_embedding(item_type_ids)
        )

        batch_size = x.size(0)
        cls_tokens = self.cls_token.expand(batch_size, -1, -1)
        x = torch.cat([cls_tokens, x], dim=1)
        x = self.position_encoding(x)
        x = self.dropout(x)

        cls_padding = torch.zeros((batch_size, 1), dtype=torch.bool, device=padding_mask.device)
        key_padding_mask = torch.cat([cls_padding, padding_mask], dim=1)
        causal_mask = self._build_causal_mask(seq_len=x.size(1), device=x.device)
        encoded = self.encoder(x, mask=causal_mask, src_key_padding_mask=key_padding_mask)
        pooled = encoded[:, 0]

        return {
            "location_logits": self.location_head(pooled),
            "time_logits": self.time_head(pooled),
        }

    @staticmethod
    def _build_causal_mask(seq_len: int, device: torch.device) -> torch.Tensor:
        mask = torch.full((seq_len, seq_len), float("-inf"), device=device)
        return torch.triu(mask, diagonal=1)
