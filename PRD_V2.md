# Chai Bethak Bar POS — Version 2 Product Requirements Document

**Document:** PRD V2  
**Product:** Chai Bethak Bar POS  
**Existing application:** Kotlin Android POS  
**Package ID:** `com.pinankh.chaibethakbar`  
**Platform:** Android tablet, 10-inch or larger, landscape  
**Architecture:** Jetpack Compose + Material 3 + MVVM + StateFlow + Repository + Firebase Cloud Firestore  
**Authentication:** Existing Firebase email/password authentication  
**Database:** Existing Firestore project with persistent offline cache  
**Currency:** INR (₹), stored in paise  
**Operators:** Two co-owners  

---

# 1. Product Overview

## 1.1 Purpose

Version 2 extends the existing Chai Bethak Bar POS into a café operations and inventory management application.

The existing application already supports:
- Dine-in table management.
- Takeaway and parcel orders.
- Menu management.
- Cash and online settlement.
- Digital bills.
- Daily sales reports.
- Outlet configuration.

Version 2 adds:
- Dynamic category management.
- Inventory item management.
- Supplier management.
- Purchase entry and purchase history.
- Supplier credit and payment tracking.
- Stock management and stock adjustments.
- Automatic stock deduction after sales.
- Recipe-based ingredient deduction.
- Purchase, stock and business reports.

## 1.2 Core business requirements

The café owners purchase different kinds of goods, including:

- Sandwich bread.
- Garlic bread.
- Pizza bread.
- Burger buns.
- French fries in different packet sizes.
- Mayonnaise.
- Sauces.
- Cheese.
- Food packaging and packets.
- Cold drinks.
- Cigarettes.
- Gutka.
- Other food ingredients, consumables and resale products.

The application must record purchases, maintain inventory, track supplier payments and calculate stock consumption from sales.

**Important:** A purchased inventory item is not necessarily a menu item. Ingredients, packaging materials and products sold directly to customers must be supported separately.

## 1.3 Version 2 design principles

1. Preserve all six existing implementation phases.
2. Do not rewrite working billing or settlement functionality.
3. Maintain the existing Kotlin and Jetpack Compose architecture.
4. Continue using Firestore persistent offline cache.
5. Use Long paise for all monetary values.
6. Use precise integer-based base units for inventory quantities.
7. Keep purchase and stock movement history auditable.
8. Support both co-owners using the same outlet.
9. Provide tablet-friendly Material 3 interfaces.
10. Implement new modules incrementally, with tests and builds after every phase.

---

# 2. Version 2 Navigation

Add the following navigation destinations without removing existing screens.

## 2.1 Existing modules

- POS Home.
- Dine-in and takeaway order workspace.
- Menu Manager.
- Table Manager.
- Outlet Settings.
- Daily Sales Reports.

## 2.2 New modules

**Inventory & Purchases**
- Inventory Dashboard.
- Purchase Entry.
- Purchase History.
- Inventory Items.
- Stock Overview.
- Stock Adjustments.
- Suppliers.
- Supplier Payments.

**Category & Menu Management**
- Category Manager.
- Existing Menu Manager.
- Recipe Management (Phase 12).

**Reports**
- Existing Daily Sales Reports.
- Purchase Reports.
- Stock Reports.
- Supplier Outstanding Reports.
- Gross Profit Reports.

The navigation must remain easy to use on a 10-inch landscape tablet.

---

# 3. Category Management

## 3.1 Objective

Replace fixed category handling with Firestore-backed dynamic categories.

The existing categories must be migrated without losing existing menu item assignments.

## 3.2 Category fields

Collection: `categories`

| Field | Type | Description |
|---|---|---|
| categoryId | String | Document ID |
| name | String | Category name |
| description | String | Optional description |
| categoryType | String | MENU or INVENTORY |
| isActive | Boolean | Active status |
| sortOrder | Int | Display order |
| createdAt | Timestamp | Creation date |
| updatedAt | Timestamp | Last update |

Menu and inventory categories must be distinguishable.

Example menu categories:
- Chai.
- Coffee.
- Snacks.
- Fast Food.
- Mocktails.
- Combos.
- Cold Drinks.
- Tobacco Products, if applicable to the outlet's permitted sales.

Example inventory categories:
- Bakery Products.
- Dairy Products.
- Frozen Food.
- Sauces and Condiments.
- Packaging Materials.
- Beverages.
- General Consumables.
- Other Raw Materials.

## 3.3 Category operations

- Add category.
- Edit category.
- Enable or disable category.
- Change display order.
- Search categories.
- Delete a category only when no active inventory or menu items depend on it.
- Prevent deletion of categories used by historical records.
- Provide a clear message when deletion is blocked.

Category names must not be hardcoded in the application.

---

# 4. Inventory Item Management

## 4.1 Objective

Create a separate inventory catalogue for all purchased products, ingredients, consumables and resale products.

An inventory item may be connected to a menu item later through recipe mapping, but it must not automatically become a POS menu item.

## 4.2 Inventory item types

Each inventory item must support one of the following types:

**INGREDIENT**
- Bread.
- Cheese.
- Mayonnaise.
- Sauces.
- French fries.

**RESALE_PRODUCT**
- Bottled cold drinks.
- Packaged food products.
- Other products sold directly to customers.

**PACKAGING**
- Food packets.
- Sandwich wrappers.
- Takeaway containers.

**CONSUMABLE**
- Cleaning materials.
- Other café operating supplies.

**OTHER**
- Any additional inventory item.

## 4.3 Inventory item fields

Collection: `inventory_items`

| Field | Type | Description |
|---|---|---|
| itemId | String | Firestore document ID |
| name | String | Item name |
| categoryId | String | Inventory category |
| itemType | String | INGREDIENT / RESALE_PRODUCT / PACKAGING / CONSUMABLE / OTHER |
| baseUnit | String | g, ml, piece, bottle, packet, etc. |
| purchaseUnit | String | Default purchase unit |
| conversionFactor | Long | Number of base units per purchase unit |
| defaultPurchaseRatePaise | Long | Last/default purchase rate |
| minimumStockBaseUnits | Long | Low-stock threshold |
| currentStockBaseUnits | Long | Current stock projection |
| isActive | Boolean | Active status |
| notes | String | Optional notes |
| createdAt | Timestamp | Creation date |
| updatedAt | Timestamp | Last update |

Every field must have a safe default value for Firestore deserialization.

## 4.4 Different sizes and packaging

The system must support different supplier packaging sizes.

Example: French Fries.

| Purchase option | Base unit conversion |
|---|---:|
| 500 g packet | 500 g |
| 1 kg packet | 1,000 g |
| 2 kg packet | 2,000 g |

If the owner purchases two 1 kg packets, the inventory increases by 2,000 g.

Example: Mayonnaise.

- Purchase unit: 1 kg pouch.
- Base unit: grams.
- Conversion factor: 1,000.

Example: Cold drinks.

- Purchase unit: crate.
- Base unit: bottle.
- Conversion factor: actual number of bottles per crate.

Example: Food packets.

- Purchase unit: bundle.
- Base unit: piece.
- Conversion factor: actual number of packets per bundle.

The owner must be able to define these conversions while creating or editing an inventory item.

Do not use floating-point values for stored stock quantities. Use integer base units.

For fractional recipe quantities, use a sufficiently small base unit, such as milligrams or millilitres, when practical.

## 4.5 Inventory item operations

- Add inventory item.
- Edit inventory item.
- Enable/disable item.
- Search and filter.
- View purchase history.
- View current stock.
- View stock movement history.
- Set minimum stock level.
- View low-stock status.

Do not delete an item with existing purchase or stock movement history. Archive or deactivate it instead.

---

# 5. Supplier Management

## 5.1 Objective

Maintain supplier details and track purchases and outstanding payments.

## 5.2 Supplier fields

Collection: `suppliers`

| Field | Type | Description |
|---|---|---|
| supplierId | String | Document ID |
| supplierName | String | Supplier/business name |
| contactPerson | String | Optional |
| phone | String | Contact number |
| address | String | Optional |
| gstin | String | Optional GSTIN |
| openingBalancePaise | Long | Opening payable balance |
| notes | String | Optional notes |
| isActive | Boolean | Supplier status |
| createdAt | Timestamp | Creation date |
| updatedAt | Timestamp | Last update |

## 5.3 Supplier operations

- Add supplier.
- Edit supplier.
- Search suppliers.
- View supplier details.
- View purchase history.
- View total purchases.
- View total payments.
- View outstanding payable balance.
- Record supplier payment.
- View payment history.

A supplier must not be deleted if historical purchases or payments reference that supplier.

---

# 6. Purchase Management

## 6.1 Objective

Allow owners to record one purchase invoice containing multiple inventory items.

A purchase must increase stock only when it is successfully posted.

## 6.2 Purchase Entry screen

The screen must contain:

**Purchase header**
- Automatically generated purchase number.
- Purchase date and time.
- Supplier selection.
- Supplier invoice number (optional).
- Payment method.
- Notes.

**Purchase item list**
- Select inventory item.
- Purchase quantity.
- Purchase unit.
- Conversion to base unit.
- Purchase rate.
- Discount.
- Tax type and tax amount, where applicable.
- Line total.

The owner must be able to add multiple items to a single purchase.

## 6.3 Purchase example

Illustrative purchase:

| Item | Quantity | Unit | Total |
|---|---:|---|---:|
| Sandwich Bread | 10 | Pack | ₹500 |
| Garlic Bread | 5 | Pack | ₹400 |
| Pizza Bread | 20 | Piece | ₹600 |
| Burger Bun | 30 | Piece | ₹450 |
| French Fries | 5 | Kg | ₹750 |
| Mayonnaise | 2 | Kg | ₹360 |
| Cheese | 3 | Kg | ₹1,200 |
| Food Packets | 100 | Piece | ₹300 |
| **Total** | | | **₹4,560** |

The example is only for demonstrating the workflow.

## 6.4 Purchase calculations

All financial values must be stored in Long paise.

For every line:

- Calculate quantity in purchase units.
- Convert quantity into base inventory units.
- Calculate line subtotal.
- Apply item-level discount.
- Calculate applicable purchase tax.
- Calculate final line amount.

Purchase totals:

- Subtotal.
- Discount.
- Tax.
- Other charges, if entered.
- Grand total.
- Paid amount.
- Outstanding amount.

Use integer paise arithmetic. Do not use Double or Float for money.

Tax configuration must allow the owner to record the applicable supplier invoice treatment. Do not assume that all purchase taxes are recoverable input tax credit. Accounting/tax treatment must remain separately configurable and subject to the owner's accountant's advice.

## 6.5 Purchase payment methods

Purchase payments are separate from customer payment methods.

Support:

- CASH.
- ONLINE.
- CREDIT.

Also support partial payment.

Examples:

**Fully paid purchase**
- Purchase total: ₹4,560.
- Paid: ₹4,560.
- Outstanding: ₹0.
- Status: PAID.

**Partially paid purchase**
- Purchase total: ₹4,560.
- Paid: ₹2,000.
- Outstanding: ₹2,560.
- Status: PARTIALLY_PAID.

**Credit purchase**
- Purchase total: ₹4,560.
- Paid: ₹0.
- Outstanding: ₹4,560.
- Status: UNPAID.

## 6.6 Purchase statuses

- DRAFT.
- POSTED.
- PARTIALLY_PAID.
- PAID.
- CANCELLED.

Rules:

1. Draft purchases do not affect stock.
2. Posting a purchase increases stock exactly once.
3. Payment recording updates the supplier balance.
4. A posted purchase must never be silently deleted.
5. Cancellation requires a reason and a corresponding reversal stock movement.
6. Purchase history must remain available.
7. Editing a posted purchase must use an auditable correction/reversal process rather than silently changing historical stock.

---

# 7. Purchase Data Model

Collection: `purchases`

Suggested fields:

| Field | Type |
|---|---|
| purchaseId | String |
| purchaseNo | String |
| supplierId | String |
| supplierNameSnapshot | String |
| supplierInvoiceNo | String |
| purchaseDate | Timestamp |
| items | List of PurchaseItem |
| subtotalPaise | Long |
| discountPaise | Long |
| taxPaise | Long |
| additionalChargesPaise | Long |
| totalPaise | Long |
| paidPaise | Long |
| outstandingPaise | Long |
| paymentStatus | String |
| status | String |
| notes | String |
| createdAt | Timestamp |
| updatedAt | Timestamp |
| postedAt | Timestamp? |

PurchaseItem must snapshot:

- inventoryItemId.
- itemName.
- categoryName.
- purchaseUnit.
- quantity.
- conversionFactor.
- baseQuantity.
- purchaseRatePaise.
- discountPaise.
- taxType.
- taxPercent.
- taxPaise.
- lineTotalPaise.

Historical purchase records must remain unchanged when an inventory item or supplier is renamed.

---

# 8. Supplier Payments and Credit Ledger

## 8.1 Objective

Allow the owners to pay suppliers immediately or later.

## 8.2 Supplier payment fields

Collection: `supplier_payments`

- paymentId.
- supplierId.
- supplierNameSnapshot.
- purchaseId (optional).
- paymentDate.
- paymentMode: CASH or ONLINE.
- amountPaise.
- referenceNumber (optional).
- notes.
- createdAt.

## 8.3 Payment rules

- Allow partial payments.
- Allow payments against a particular purchase.
- Support supplier advance payments.
- Display remaining outstanding balance.
- Prevent payment amounts from being negative.
- Prevent accidental overpayment against an invoice unless the payment is intentionally recorded as supplier advance.
- Never delete historical payment entries.
- Corrections must be recorded as reversal/adjustment entries.

## 8.4 Supplier account summary

Display:

- Opening balance.
- Total purchases.
- Total payments.
- Purchase returns or credit adjustments, if supported.
- Outstanding payable balance.

The outstanding amount must be calculated from the ledger and reconciled against purchase records.

---

# 9. Stock Management

## 9.1 Objective

Maintain accurate stock balances and a complete movement history.

## 9.2 Stock movement types

Collection: `stock_movements`

Supported movement types:

- OPENING_STOCK.
- PURCHASE_IN.
- SALE_OUT.
- STOCK_ADJUSTMENT_IN.
- STOCK_ADJUSTMENT_OUT.
- DAMAGE_OUT.
- EXPIRY_OUT.
- PURCHASE_RETURN_OUT.
- PURCHASE_REVERSAL_OUT.
- SALE_REVERSAL_IN.

## 9.3 Stock movement fields

| Field | Type |
|---|---|
| movementId | String |
| inventoryItemId | String |
| itemNameSnapshot | String |
| movementType | String |
| quantityBaseUnits | Long |
| signedQuantityBaseUnits | Long |
| referenceType | String |
| referenceId | String |
| notes | String |
| createdAt | Timestamp |
| createdBy | String |
| syncStatus | String |

The signed quantity is positive for stock-in and negative for stock-out.

## 9.4 Stock adjustment

Provide a stock adjustment screen for:

- Physical stock correction.
- Damaged products.
- Expired products.
- Wastage.
- Missing stock.
- Opening stock.

Every adjustment requires:

- Inventory item.
- Adjustment type.
- Quantity.
- Reason.
- Date.
- Optional notes.

The system must record the movement instead of directly overwriting historical stock.

## 9.5 Low-stock alerts

Compare current stock with the configured minimum stock level.

Show:

- Available stock.
- Minimum stock.
- Stock status.
- Suggested purchase quantity (optional future enhancement).

Low-stock items should be visible on the Inventory Dashboard.

---

# 10. Inventory Dashboard

The Inventory Dashboard must display:

- Total active inventory items.
- Total low-stock items.
- Total stock adjustments.
- Recent purchases.
- Recent stock movements.
- Supplier outstanding balance.
- Searchable inventory list.

Each inventory item card should show:

- Item name.
- Category.
- Available stock.
- Unit.
- Minimum stock.
- Low-stock indicator.

Selecting an item opens its detail screen with purchase history and stock movement history.

---

# 11. Automatic Stock Deduction

## 11.1 Objective

Stock should be reduced automatically when a customer order is successfully settled.

This is separate from recipe-based deduction, which is introduced in Phase 12.

## 11.2 Direct resale products

For a directly sold product such as a bottled cold drink:

- Menu item is mapped to one inventory item.
- Selling one bottle reduces inventory by one bottle.
- Selling five bottles reduces inventory by five bottles.

## 11.3 Initial automatic deduction support

Before Phase 12, support direct product mapping for resale products.

Ingredient-based deduction must wait until recipe management is implemented.

## 11.4 Settlement integration

When a POS order becomes PAID:

- Identify mapped resale inventory products.
- Calculate quantities sold.
- Create corresponding SALE_OUT movements.
- Update the stock projection.
- Associate movements with the original order ID.
- Ensure the same order cannot deduct stock twice.

If an order is cancelled before settlement, it must not deduct stock.

If a paid order is subsequently reversed through an authorized correction, the application must record the corresponding SALE_REVERSAL_IN movement.

Do not modify the existing customer payment modes. They remain strictly ONLINE and CASH.

---

# 12. Recipe Management — Phase 12

## 12.1 Objective

Allow owners to define ingredients and quantities required to prepare a menu item.

This enables automatic ingredient consumption after a successful sale.

## 12.2 Recipe example

Menu item: Cheese Sandwich.

| Ingredient | Quantity per sandwich |
|---|---:|
| Sandwich Bread | 2 slices |
| Cheese | 30 g |
| Mayonnaise | 10 g |
| Sauce | 5 g |

Selling 10 sandwiches consumes:

- 20 bread slices.
- 300 g cheese.
- 100 g mayonnaise.
- 50 g sauce.

## 12.3 Recipe fields

Collection: `recipes`

- recipeId.
- menuItemId.
- menuItemNameSnapshot.
- ingredients.
- isActive.
- createdAt.
- updatedAt.

Each ingredient contains:

- inventoryItemId.
- inventoryItemNameSnapshot.
- quantityBaseUnits.
- baseUnit.

## 12.4 Recipe operations

- Create recipe.
- Edit recipe.
- Add/remove ingredients.
- Set ingredient quantities.
- Enable/disable recipe.
- View estimated ingredient consumption.
- View stock availability.

## 12.5 Sale integration

When a menu item is settled:

- Read its active recipe.
- Multiply ingredient quantities by sold quantity.
- Generate stock-out movements.
- Associate all movements with the order ID.
- Prevent duplicate deduction.
- Preserve the recipe snapshot used for the sale.

If a recipe is edited later, historical sales and stock movements must not be recalculated.

---

# 13. Purchase, Stock and Business Reports

Add the following reports without changing existing Daily Sales Reports.

## 13.1 Purchase Report

Filters:
- Today.
- This week.
- This month.
- Custom date range.
- Supplier.

Display:
- Total purchases.
- Total paid.
- Total outstanding.
- Purchase count.
- Supplier-wise purchase summary.

## 13.2 Stock Report

Display:
- Current stock.
- Stock movement history.
- Low-stock items.
- Damaged stock.
- Expired stock.
- Stock adjustments.

## 13.3 Supplier Outstanding Report

Display:
- Supplier name.
- Total purchases.
- Total payments.
- Outstanding balance.
- Last payment date.

## 13.4 Gross Profit Report

After Phase 12, calculate estimated gross profit:

Revenue excluding applicable taxes  
minus cost of ingredients consumed  
minus cost of directly resold products.

The calculation must use historical purchase cost information and the selected inventory valuation method.

Recommended initial method: weighted average cost.

Clearly label this as estimated gross profit, not net profit. Rent, electricity, wages, other expenses and tax/accounting adjustments are outside this calculation unless a future expense module is introduced.

---

# 14. Firestore Collection Structure

Keep the existing collections intact:

- `outlet_config`
- `tables`
- `menu_items`
- `orders`

Add:

- `categories`
- `inventory_items`
- `suppliers`
- `purchases`
- `supplier_payments`
- `stock_movements`
- `recipes` (Phase 12)

All new documents must use default values for Kotlin model fields.

All historical purchase, payment, order and stock movement documents must be retained.

## 14.1 Outlet separation

The current application is designed for one outlet.

Version 2 must continue using the existing outlet configuration and data scope. However, the implementation must not assume that requiring Firebase authentication alone isolates one outlet from another.

Before production deployment of the new collections, review Firestore security rules and ensure both authorized co-owners can access the correct outlet's records while unrelated authenticated users cannot access them.

---

# 15. Offline-First and Data Integrity Requirements

This section is especially important.

The application must continue supporting offline POS operations.

However, inventory introduces more complex concurrent updates than normal order entry.

## 15.1 Offline behavior

- Purchase drafts can be created offline.
- Stock movements can be queued locally.
- POS settlements must remain usable offline.
- Pending stock movements must show a visible pending-sync status.
- Reconnection must synchronize pending operations.
- The application must not silently lose stock movements.
- Failed synchronization must show a recoverable error state.

## 15.2 Idempotency

Every stock movement must have a stable unique operation ID.

Purchase posting, purchase cancellation, supplier payment recording and sales stock deduction must be protected against duplicate application.

Do not rely only on a button loading state to prevent duplicates.

## 15.3 Two co-owner devices

Both co-owners may access the same stock.

The application must consider:

- Simultaneous purchases.
- Simultaneous stock adjustments.
- Sales while the other tablet is offline.
- Reconnection after offline changes.
- Conflicting edits.

Firestore client-side WriteBatch is useful for atomic writes but does not provide read-dependent concurrency control or guaranteed idempotency across separately submitted retries.

Therefore:

- Do not silently overwrite stock balances using stale device values.
- Keep an append-only stock movement ledger.
- Use stable operation IDs.
- Reconcile the stock projection against the movement ledger.
- For strict multi-device correctness, use an idempotent trusted backend operation, such as a Firebase Cloud Function with server-side transactional processing, or another verified concurrency-control mechanism.
- If backend processing is not implemented, clearly document the limitations and require reconciliation before relying on the balance for purchasing decisions.

The existing rule against client-side `runTransaction` in the original POS must not be casually removed. Any new backend mechanism must be explicitly designed and tested.

## 15.4 Stock below zero

Provide configurable behavior:

- Warn when a sale would reduce stock below zero.
- Display the projected negative balance.
- Do not silently hide stock discrepancies.
- Allow the owner to choose whether to block or permit negative stock, subject to the configured business policy.

---

# 16. Security and Audit Requirements

Extend Firestore security rules to cover every new collection.

Requirements:

- Authentication required.
- Outlet-level access restricted to authorized co-owners.
- No unrestricted access to inventory or supplier records.
- Prevent unauthorized deletion of posted purchases.
- Prevent unauthorized deletion of supplier payments.
- Protect stock movement history.
- Validate supported statuses and numeric fields where possible.
- Never expose service account credentials in the Android application.

Sensitive commercial information such as supplier contact details and outstanding balances must be available only to authorized users.

Tobacco-related inventory records, if used, are for business stock recording only. The application must not bypass applicable age, sale or regulatory restrictions.

---

# 17. User Interface Requirements

All new screens must follow the existing Material 3 café design.

Requirements:

- Landscape tablet-first layout.
- High-contrast readable text.
- Minimum 48.dp touch targets.
- Clear loading and empty states.
- Search and filter support.
- Confirmation for destructive or financial actions.
- No unnecessary navigation complexity.
- Preserve existing POS screen layout and behavior.
- Use Long paise for displayed monetary calculations.
- Display inventory quantities with their units.

Purchase entry must be optimized for quick multi-item entry.

The owner should be able to add multiple purchase lines without repeatedly opening and closing separate screens.

---

# 18. Version 2 Implementation Roadmap

The original six phases remain completed and unchanged.

The following are additional phases.

## Phase 7 — Dynamic Category Management

Implement:
- Firestore-backed category model.
- Category repository.
- Category ViewModel.
- Category Manager screen.
- Add/edit/disable/delete safeguards.
- Migration of existing menu categories.
- Category filtering in Menu Manager.

Tests:
- Category creation.
- Category update.
- Category disable.
- Category deletion protection.
- Existing menu item compatibility.

Build:
- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

## Phase 8 — Inventory Item Management

Implement:
- Inventory item model.
- Inventory categories.
- Base unit and purchase unit conversion.
- Different packaging sizes.
- Inventory item CRUD.
- Opening stock.
- Inventory dashboard.
- Low-stock thresholds.

Tests:
- Unit conversion.
- Base stock calculations.
- Different pack sizes.
- Invalid quantities.
- Inventory item validation.

Build and test after completion.

## Phase 9 — Supplier Management

Implement:
- Supplier model.
- Supplier CRUD.
- Supplier detail screen.
- Supplier purchase history.
- Opening payable balance.
- Supplier search and filters.

Tests:
- Supplier creation.
- Supplier update.
- Historical supplier snapshot.
- Supplier deletion protection.

Build and test after completion.

## Phase 10 — Purchase Entry and Purchase History

Implement:
- Purchase header and item models.
- Multi-item purchase entry.
- Purchase draft.
- Purchase posting.
- Purchase history.
- Purchase details.
- Purchase tax and discount calculations.
- Purchase payment status.
- Stock-in movements.
- Purchase cancellation/reversal.

Tests:
- Multi-item purchase.
- Purchase total calculation.
- Base unit conversion.
- Draft does not affect stock.
- Posting increases stock once.
- Duplicate posting prevention.
- Cancellation reversal.
- Historical snapshot integrity.

Build and test after completion.

## Phase 11 — Stock Management and Supplier Payments

Implement:
- Stock movement ledger.
- Current stock projection.
- Stock adjustment.
- Damaged/expired stock.
- Low-stock alerts.
- Supplier payments.
- Partial payments.
- Credit purchases.
- Supplier outstanding balances.
- Stock synchronization and reconciliation.

Tests:
- Stock-in.
- Stock-out.
- Adjustment.
- Damage and expiry.
- Partial supplier payments.
- Credit balance calculations.
- Duplicate movement prevention.
- Offline/reconnect behavior.
- Two-device conflict scenarios.

Build and test after completion.

## Phase 12 — Recipe Management and Sales Integration

Implement:
- Recipe model.
- Recipe editor.
- Ingredient mapping.
- Direct resale item mapping.
- Automatic ingredient deduction.
- Automatic resale product deduction.
- Historical recipe snapshots.
- Stock reversal for authorized sale corrections.
- Weighted average cost support.

Tests:
- One menu item with multiple ingredients.
- Multiple sold quantities.
- Ingredient consumption.
- Direct resale deduction.
- Insufficient stock.
- Duplicate sale deduction prevention.
- Recipe changes must not modify historical consumption.

Build and test after completion.

## Phase 13 — Purchase, Inventory and Business Reports

Implement:
- Purchase reports.
- Supplier reports.
- Outstanding balances.
- Stock reports.
- Low-stock reports.
- Stock movement history.
- Estimated gross profit.
- Date and supplier filters.

Tests:
- Date filtering.
- Supplier aggregation.
- Purchase totals.
- Outstanding balances.
- Stock valuation.
- Estimated gross profit.
- Empty report scenarios.

Run full application verification.

---

# 19. Version 2 Acceptance Criteria

Version 2 will be accepted only when:

1. Existing six-phase POS functionality still works.
2. Existing orders and historical bills remain intact.
3. Categories can be managed dynamically.
4. Inventory items can be created with different purchase sizes.
5. Suppliers can be maintained.
6. Multiple items can be entered in one purchase.
7. Purchase posting increases stock exactly once.
8. Credit purchases update outstanding supplier balances.
9. Partial supplier payments are supported.
10. Stock adjustments create audit records.
11. Direct resale products deduct stock after successful settlement.
12. Recipe-based ingredients deduct stock after successful settlement.
13. Offline operations synchronize without silently losing movements.
14. Two-device conflicts are handled or clearly surfaced.
15. Reports reconcile with purchase and stock movement records.
16. All financial calculations use Long paise.
17. Automated tests and release builds pass.
18. Firestore security rules protect the new data.

---

# 20. AI Agent Implementation Instructions

The Android Studio AI Agent must follow these rules:

1. Read the original PRD and this Version 2 PRD before implementation.
2. Preserve Phases 1–6.
3. Do not rewrite working POS functionality.
4. Implement only one new phase per command.
5. Inspect existing models and repository methods before modifying them.
6. Reuse the existing Material 3 theme and architecture.
7. Avoid duplicate models and repositories.
8. Do not hardcode inventory categories or units.
9. Do not use Double or Float for money.
10. Use integer base units for inventory.
11. Preserve historical purchase, payment, order and stock movement records.
12. Use stable IDs and idempotent posting operations.
13. Document any backend functionality that cannot be safely implemented through the Android client alone.
14. Write unit tests for all calculation logic.
15. Run unit tests and assembleDebug at the end of each phase.
16. Report changed files, tests, build results and unresolved risks.
17. Do not claim a physical-device or Firebase integration test passed unless it was actually performed.
18. Do not deploy Firestore rules or publish the app without explicit approval.

---

# 21. Final Version 2 Scope

| Module | Implementation phase |
|---|---:|
| Existing POS and billing | Phases 1–6, preserved |
| Dynamic categories | 7 |
| Inventory item management | 8 |
| Supplier management | 9 |
| Purchase entry and history | 10 |
| Stock management and supplier payments | 11 |
| Recipe and automatic ingredient deduction | 12 |
| Purchase, stock and business reports | 13 |

**Final product direction:**

Chai Bethak Bar POS will evolve from a paperless billing application into an integrated café operations management system, supporting sales, purchases, inventory, suppliers, stock consumption and business reporting.

The existing POS remains the foundation. Version 2 adds the operational modules around it without disrupting its established workflows.