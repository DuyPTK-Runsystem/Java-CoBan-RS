# Plan 081 — Placement seed rows

### B.1 Placement candidates

| Candidate key | Student      | Source class | Score snapshot | Gender snapshot | Scenario                |
| ------------- | ------------ | ------------ | -------------: | --------------- | ----------------------- |
| `PLC-STU-01`  | `STU2600001` | `6A1`        |            9.5 | `MALE`          | đủ dữ liệu, ưu tiên cao |
| `PLC-STU-02`  | `STU2600002` | `6A1`        |            8.8 | `FEMALE`        | đủ dữ liệu              |
| `PLC-STU-03`  | `STU2600003` | `6A1`        |            8.8 | `MALE`          | tie score               |
| `PLC-STU-04`  | `STU2600004` | `6A1`        |            7.0 | `FEMALE`        | đủ dữ liệu              |
| `PLC-STU-05`  | `STU2600005` | `6A2`        |           null | `MALE`          | thiếu score             |
| `PLC-STU-06`  | `STU2600006` | `6A2`        |            6.5 | null            | thiếu gender            |
| `PLC-STU-07`  | `STU2600007` | `6A2`        |            5.0 | `FEMALE`        | có lịch sử score        |
| `PLC-STU-08`  | `STU2600008` | `6A2`        |            4.5 | `MALE`          | có attendance           |
| `PLC-STU-09`  | `STU2600009` | `7A1`        |            9.0 | `FEMALE`        | capacity cutoff         |
| `PLC-STU-10`  | `STU2600010` | `7A1`        |            9.0 | `MALE`          | capacity cutoff tie     |
| `PLC-STU-11`  | `STU2600011` | `7A1`        |            7.5 | `FEMALE`        | manual review           |
| `PLC-STU-12`  | `STU2600012` | `7A1`        |            7.2 | `MALE`          | enrollment conflict     |


