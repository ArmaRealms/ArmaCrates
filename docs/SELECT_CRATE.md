# SelectCrate - User Selection Crate Type

## Overview
SelectCrate is a new crate type for CrazyCrates that allows players to **choose** which prize they want to receive instead of relying on random chance.

## How It Works

1. **Opening the Crate**: When a player opens a SelectCrate (by right-clicking a physical crate or using `/crates open <crate>`), they are presented with a GUI showing all available prizes.

2. **Selecting Prizes**: Click to select a reward and click its marker again to deselect it. Select up to `floor(available keys / max(1, RequiredKeys))` distinct rewards. With five keys and `RequiredKeys: 1`, select up to five rewards. Selections persist across pages. The confirmation button always shows the selected count and current limit. At the limit, unselected rewards show an unavailable notice.

3. **Confirming**: Click the confirmation button (default slot 49 in a 54-slot menu). The plugin revalidates the current crate, selections, key balance and inventory. Each selected reward costs `max(1, RequiredKeys)` keys. Free openings allow one reward. The opening key source is preserved: physical and virtual keys are not combined. When opening from held keys, only the main hand and offhand count; other physical openings use storage and offhand.

4. **Receiving Prizes**: All native reward items are built once, including permission-based alternative rewards. The plugin prepares a detached inventory containing the exact key debit and all reward items, respecting stack sizes, quantities and item metadata. Armor and offhand are not reward storage. Slots freed by physical keys can receive rewards. If the whole batch cannot fit, nothing is consumed or delivered and the player sees: “Você não possui espaço suficiente no inventário para receber todos os produtos selecionados.”

5. **Canceling**: Click Cancel (default slot 53), close the inventory, or disconnect. No keys or rewards change. A confirmation queued before the menu closes is discarded. Repeated confirmation clicks can redeem the session only once.

## Delivery limits

Use `Items` or editor item stacks for inventory-validated rewards. Existing custom item support, including Oraxen's `or:` syntax, uses the same prepared delivery.

Reward commands remain supported and run once per selected reward after the native items commit. Commands and event listeners belong to other plugins: their delivered items cannot be predicted for the inventory check, and their effects cannot be rolled back. If a command fails, do not retry the entire redemption automatically. Later rewards still receive their callback attempt. Thrown callback exceptions are logged; command dispatch otherwise retains its existing error handling. Server crashes between file persistence and player saving are not a durable cross-file transaction.

## Configuration

### Basic Setup

```yaml
Crate:
  CrateType: SelectCrate
  CrateName: '&6&lSelect Crate'
  RequiredKeys: 1
  
  SelectCrate:
    GUI:
      Size: 54  # Must be 9, 18, 27, 36, 45, or 54
      Title: '&6Select Your Prize!'
    
    Confirm:
      Slot: 49  # Center of the bottom row in a 54-slot inventory
      Item:
        Material: LIME_CONCRETE
        Name: '&aConfirm Choice'
        Lore:
          - '&7Click to receive all selected prizes.'

    Cancel:
      Slot: 53

    Navigation:
      Previous:
        Slot: 48
        Item:
          Material: ARROW
          Name: '&ePrevious Page'
          Lore:
            - '&7Current page: %page% of %pages%'
      Next:
        Slot: 50
        Item:
          Material: ARROW
          Name: '&aNext Page'
          Lore:
            - '&7Current page: %page% of %pages%'
    
    SelectionMarker:
      Material: NETHER_STAR
      Name: '&e&l✓ SELECTED'
      Lore:
        - '&7This prize is selected.'
    
    Messages:
      NoSelection: '&cSelecione ao menos um produto antes de confirmar!'
      Counter: '&eSelecionados: %selected%/%limit%'
      Cancel: '&cCancelar'
      SelectionLimit: '&cVocê atingiu o limite de seleções. Desmarque um produto para escolher outro.'
      Unavailable: '&cLimite atingido. Desmarque outro produto.'
      InventoryFull: '&cVocê não possui espaço suficiente no inventário para receber todos os produtos selecionados.'
      InvalidReward: '&cNão foi possível preparar os produtos. Avise a equipe.'
```

### Prize Configuration

Prizes in SelectCrate work the same as other crate types, but MaxRange and Chance are not used since all prizes are available for selection:

```yaml
  Prizes:
    1:
      DisplayName: '&e&lDiamond Sword'
      DisplayItem: 'DIAMOND_SWORD'
      DisplayAmount: 1
      Lore:
        - '&7A powerful sword!'
      Items:
        - 'Item:DIAMOND_SWORD, Amount:1, Enchantments:DAMAGE_ALL-5'
    
    2:
      DisplayName: '&6&lMoney Bundle'
      DisplayItem: 'EMERALD'
      DisplayAmount: 32
      Lore:
        - '&7Receive $10,000!'
      Commands:
        - 'eco give %player% 10000'
```

## Important Notes

1. **GUI Size**: Reserve the bottom row (slots 45-53 in a 54-slot GUI) for controls. Prizes that do not fit are split into pages. Controls outside the reserved row or overlapping another control fall back to separate slots. A 9-slot menu has one prize slot.

2. **Required Keys**: The configured cost applies to each selected reward. Zero and negative values retain the previous minimum cost of one key.

3. **No Random Selection**: Unlike other crate types, **all prizes are available** to choose from. The Chance and MaxRange values are ignored.

4. **Session Safety**: If a player disconnects or the server restarts while they have a SelectCrate open, no keys are consumed and the session is cleaned up automatically.

5. **Inventory Protection**: Players cannot move items in the SelectCrate GUI. All inventory interactions except selection are blocked.

## Example Use Cases

- **VIP Reward Crates**: Let VIP players choose their monthly reward
- **Event Prizes**: Allow event winners to select their preferred prize
- **Rank Kits**: Let players pick their starting kit when ranking up
- **Donation Rewards**: Give donors choice in what they receive

## Compatibility

SelectCrate supports the following integrations:

- Physical and virtual keys
- PlaceholderAPI placeholders
- Commands as rewards, with the delivery limits above
- Multiple items per prize
- Custom messages
- Holograms
- Preview GUI

## Runtime acceptance checklist

Automated tests use Paper API doubles; run the following on a real server with both a Java client and a Bedrock client through Geyser before production acceptance:

- Five keys allow five distinct rewards, reject a sixth, and deselecting releases a choice.
- Select rewards across pages; verify the counter, markers, unavailable notice and navigation.
- Confirm a batch with partial stacks, custom metadata, unstackable items and insufficient space.
- Change physical/virtual balances while the menu is open, including main hand and offhand.
- Cancel, disconnect and close after clicking Confirm; check keys and items remain unchanged when canceled.
- Repeatedly click Confirm; verify exact per-reward debit and one item/command/event delivery.

## See Also

- Full example configuration: `SelectCrateExample.yml`
- CrazyCrates Wiki: https://docs.crazycrew.us/crazycrates/home
