# Changelog v3.3.2

## New Features
- **Calculated Grades UI**: 
  - Added "Σ Calculated" badge with purple styling for calculated grade items.
  - Replaced assignment icon with an orange Sigma (Σ) icon for calculated items.
  - Calculated grade cards now have a soft green background (#E8F5E9) for better distinction.
- **Formula Editor**:
  - Added a **Help Button** next to the formula input field.
  - Added comprehensive documentation for available functions (`avg`, `max`, `min`) and operators.
- **New Formula Variables**:
  - Added attendance-based variables for calculations:
    - `abs-td`: Count of TD absences
    - `abs-tp`: Count of TP absences
    - `pres-cours`: Count of Course presences
    - `tot-td`: Total TD sessions
    - `tot-tp`: Total TP sessions
    - `tot-c`: Total Course sessions

## Improvements
- **Formula Help**: Provides real-time guidance on how to write formulas including examples like penalty calculations (`20 - abs-td * 2`).
