# AI Design System
Version: 1.0

## Purpose

This document defines the official design language for the project.

Every AI Agent, developer, and contributor MUST follow this design system when creating or modifying UI.

The objective is to make every page look like one unified product instead of different pages designed by different people.

---

# Design Philosophy

The system should feel:

- Clean
- Modern
- Agricultural
- Trustworthy
- Government-grade
- Professional
- Easy to read
- Spacious
- Consistent

Avoid:

- Random colors
- Heavy shadows
- Bright gradients
- Neon colors
- Inconsistent spacing
- Different button styles
- Different border radius

---

# Color Palette

## Primary

| Name | Color | Usage |
|-------|---------|----------------|
| Primary | #2E7D32 | Main actions |
| Primary Hover | #256B29 | Hover |
| Primary Light | #E8F5E9 | Light background |

---

## Secondary

| Name | Color |
|------|---------|
| Secondary | #558B2F |
| Secondary Light | #F1F8E9 |

---

## Success

| Name | Color |
|------|---------|
| Success | #2E7D32 |
| Success Background | #E8F5E9 |

---

## Warning

| Name | Color |
|------|---------|
| Warning | #F9A825 |
| Warning Background | #FFF8E1 |

---

## Error

| Name | Color |
|------|---------|
| Error | #D32F2F |
| Error Background | #FFEBEE |

---

## Info

| Name | Color |
|------|---------|
| Info | #1976D2 |
| Info Background | #E3F2FD |

---

# Text Colors

| Component | Color |
|-----------|---------|
| Primary Text | #1F2937 |
| Secondary Text | #4B5563 |
| Description | #6B7280 |
| Disabled | #9CA3AF |
| White Text | #FFFFFF |

---

# Background Colors

| Component | Color |
|------------|---------|
| Application Background | #F5F7FA |
| Page Background | #FFFFFF |
| Card Background | #FFFFFF |
| Modal Background | #FFFFFF |
| Table Header | #F8FAFC |
| Table Row | #FFFFFF |
| Alternate Row | #F9FAFB |
| Sidebar | #FFFFFF |

---

# Border Colors

| Component | Color |
|------------|---------|
| Default Border | #E5E7EB |
| Hover Border | #CBD5E1 |
| Focus Border | #2E7D32 |

---

# Typography

## Page Header

Color

```
#111827
```

Weight

```
700
```

Size

```
32px
```

---

## Section Title

Color

```
#1F2937
```

Weight

```
600
```

Size

```
24px
```

---

## Card Title

Color

```
#1F2937
```

Weight

```
600
```

Size

```
20px
```

---

## Subtitle

Color

```
#4B5563
```

Weight

```
500
```

Size

```
18px
```

---

## Label

Color

```
#374151
```

Weight

```
500
```

Size

```
14px
```

---

## Body Text

Color

```
#374151
```

Weight

```
400
```

Size

```
16px
```

---

## Description

Color

```
#6B7280
```

Weight

```
400
```

Size

```
14px
```

---

# Buttons

## Primary Button

Background

```
#2E7D32
```

Text

```
White
```

Hover

```
#256B29
```

Border Radius

```
10px
```

Height

```
44px
```

Padding

```
16px 24px
```

---

## Secondary Button

Background

```
White
```

Border

```
#2E7D32
```

Text

```
#2E7D32
```

Hover

```
#E8F5E9
```

---

## Danger Button

Background

```
#D32F2F
```

Text

```
White
```

---

# Text Fields

Height

```
44px
```

Border

```
#D1D5DB
```

Focus

```
#2E7D32
```

Radius

```
10px
```

Padding

```
12px 16px
```

Placeholder

```
#9CA3AF
```

---

# Table

Header Background

```
#F8FAFC
```

Header Text

```
#374151
```

Row Height

```
56px
```

Alternate Row

```
#F9FAFB
```

Border

```
#E5E7EB
```

Hover

```
#F3F4F6
```

---

# Cards

Background

```
White
```

Radius

```
14px
```

Border

```
#E5E7EB
```

Shadow

```
0 2px 8px rgba(0,0,0,.04)
```

Padding

```
24px
```

---

# Layout

Page Padding

```
24px
```

Section Gap

```
24px
```

Card Gap

```
20px
```

Grid Gap

```
16px
```

---

# Icons

Default

```
#4B5563
```

Primary

```
#2E7D32
```

Danger

```
#D32F2F
```

Disabled

```
#9CA3AF
```

---

# Status Colors

| Status | Color |
|---------|---------|
| Draft | #9CA3AF |
| Pending | #F9A825 |
| Approved | #2E7D32 |
| Rejected | #D32F2F |
| Harvested | #7CB342 |
| Packaged | #1565C0 |
| Shipped | #00897B |
| Completed | #00695C |

---

# AI Agent Rules

When generating UI, the AI Agent MUST:

1. Follow this document exactly.
2. Never invent new colors unless explicitly requested.
3. Reuse existing spacing values.
4. Reuse typography hierarchy.
5. Keep every page visually consistent.
6. Prefer whitespace over visual clutter.
7. Use the Primary color only for important actions.
8. Use neutral colors for backgrounds.
9. Use status colors only for status badges.
10. Keep forms, tables, dialogs, and cards visually identical across the project.

Any generated UI that does not follow this design system should be considered incomplete and must be revised.