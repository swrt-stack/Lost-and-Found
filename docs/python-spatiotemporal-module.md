# Python Spatiotemporal Prediction Module

## Purpose

Add a pure transformer based spatiotemporal prediction module for the lost-and-found project.

## Suggested business use

- Predict the next likely lost-and-found hotspot area
- Predict the next likely active time bucket
- Support hotspot recommendation, patrol planning, and operational analysis

## Core technical design

- Input sequence:
  - location id
  - time bucket id
  - weekday id
  - item type id
- Backbone:
  - pure Transformer encoder
- Output:
  - next location top-k prediction
  - next time bucket top-k prediction

## Service endpoints

- `GET /health`
- `GET /model/status`
- `POST /predict`

## Default port

```text
8091
```
