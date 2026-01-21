![LPChestShop](https://i.imgur.com/hjjSRVi.png)

A lightweight, user-friendly chest shop plugin for Paper 1.20+ that lets players create clean, secure in-world shops using chests + signs — with a polished GUI flow, strict item matching, and Vault economy support.

## ✨ Features

- **Fast shop creation**  
  Create a shop by looking at a chest and running a command with amount + price.

- **Vault economy support**  
  Works with any Vault-compatible economy plugin.

- **Clean GUI experience**  
  Shop UIs are designed to be simple, consistent, and hard to misuse.

- **Strict item matching**  
  Shops only accept the exact configured item (same meta, name, lore, enchants, etc.) to prevent scams.

- **Stock-based shops**  
  The chest acts as the shop’s storage. Buyers can only purchase if the item is in stock.

- **Protected shop blocks**  
  Shop chests can’t be broken normally. Shops are removed by breaking the sign **twice** with an axe (anti-mistake confirmation).

- **Info & interaction**  
  - Left click on the sign or click the chest to open the **details** view  
  - Right click on the sign to **buy** instantly

- **Configurable UI**  
  Customize GUI items (material, name, lore, slots, custom model data, etc.) via config.

## 📦 Requirements

- **Paper** 1.20+ (recommended: 1.20.4+)
- **Java** 17+

## 🔗 Dependencies

Required (for economy features):
- **Vault**
- **An economy plugin** (Vault-compatible), e.g.:
  - **EssentialsX Economy** (recommended)

Notes:
- LPChestShop will run without Vault, but economy-related features will be unavailable until Vault + an economy plugin are installed.

## 🚀 Commands

> Aliases shown where applicable.

- `/lpchestshop` (alias: `/lpcs`)
  - `create <amount> <price>` — Create a shop using the item in your main hand  
  - `info` — Show info about the shop you are looking at  
  - `remove` — Remove the shop you are looking at (requires permission)  
  - `reload` — Reload config/messages (admin)

## 🔐 Permissions

- `lpchestshop.create` — Create shops (default: true)
- `lpchestshop.reload` — Reload plugin (default: op)
- `lpchestshop.remove.own` — Remove your own shop (default: true)
- `lpchestshop.remove.any` — Remove any shop (default: op)
- `lpchestshop.bypass` — Bypass restrictions (default: op)

## ⚙️ Configuration

LPChestShop is designed to be heavily configurable:
- GUI layout (slots)
- GUI item materials, names, lore
- Custom model data
- Shop restrictions (stock visibility, creation rules, limits, etc.)

## 🧠 Notes

- This plugin is intended to be **simple for players** but **strict and safe** under the hood.
- Best used with a region protection plugin if you run public markets.

## 📌 Support

If you find a bug or have a suggestion, open an issue or contact the maintainer.
