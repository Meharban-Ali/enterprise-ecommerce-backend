# DEFAULT TESTING DATASET

The default testing dataset contains standard catalog categories, products, and mock payment configurations.

## 1. Categories Catalog Data
When the database seeds with `app.data-initializer.enabled=true`, the following categories are created:

| Category ID | Category Name | Description |
| :--- | :--- | :--- |
| `1` | `Electronics` | Consumer devices and hardware |
| `2` | `Apparel` | Clothing and footwear |
| `3` | `Home & Kitchen` | Kitchen appliances and furniture |

---

## 2. Products Catalog Data
The default product entries populated on startup include:

| Product ID | Product Name | Category ID | Price | Initial Stock |
| :--- | :--- | :---: | :--- | :---: |
| `1` | `Super Phone 12` | 1 | `999.99` | 50 |
| `2` | `Wireless Earbuds` | 1 | `149.99` | 100 |
| `3` | `Classic T-Shirt` | 2 | `19.99` | 200 |
| `4` | `Coffee Maker` | 3 | `89.99` | 30 |

---

## 3. Mock Webhook Event Data
Use this mock Stripe payment payload to simulate order fulfillment:
```json
{
  "id": "evt_charge_102",
  "type": "charge.succeeded",
  "data": {
    "object": {
      "id": "ch_102",
      "amount": 19999,
      "metadata": {
        "order_id": "1"
      }
    }
  }
}
```
