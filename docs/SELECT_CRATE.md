# SelectCrate - User Selection Crate Type

## Overview
SelectCrate is a new crate type for CrazyCrates that allows players to **choose** which prize they want to receive instead of relying on random chance.

## How It Works

1. **Opening the Crate**: When a player opens a SelectCrate (by right-clicking a physical crate or using `/crates open <crate>`), they are presented with a GUI showing all available prizes.

2. **Selecting Prizes**: Click to select a reward and click its marker again to deselect it. Select up to `Crate.SelectCrate.MaxSelections` distinct rewards (default 5), while their combined key cost fits the current balance. Each prize uses its own `RequiredKeys` when configured, otherwise the crate value. With five keys, rewards costing two and three keys can be selected together. Selections persist across pages. The confirmation button shows the selected count against the count cap, and the total required keys against the current balance. Each available prize shows its individual cost. At the limit, unselected rewards show an unavailable notice.

3. **Confirming**: Click the confirmation button (default slot 49 in a 54-slot menu). The plugin revalidates the current crate, opening permission, selections, key balance and inventory. A changed prize list invalidates the displayed menu, including changes made before the first click. The key debit equals the sum of the selected prize costs. A prize override takes precedence in both directions: crate cost 1/prize cost 2 charges 2; crate cost 2/prize cost 1 charges 1. Free openings allow one reward. The opening key source is preserved: physical and virtual keys are not combined. When opening from held keys, only the main hand and offhand count; other physical openings use storage and offhand.

4. **Receiving Prizes**: All native reward items are built once. Blacklisted prizes cannot be selected, even if they define an alternative reward. The plugin prepares a detached inventory containing the exact key debit and all reward items, respecting stack sizes, quantities and item metadata. Armor and offhand are not reward storage. Slots freed by physical keys can receive rewards. If the whole batch cannot fit, nothing is consumed or delivered and the player sees: “Você não possui espaço suficiente no inventário para receber todos os produtos selecionados.”

5. **Canceling**: Click Cancel (default slot 53), close the inventory, or disconnect. No keys or rewards change. A confirmation queued before the menu closes is discarded. The session and open inventory are checked again after reward preparation, so an integration closing or replacing the menu cannot redeem the previous session. Repeated confirmation clicks can redeem the session only once.

## Delivery limits

Use `Items` or editor item stacks for inventory-validated rewards. Existing custom item support, including Oraxen's `or:` syntax, uses the same prepared delivery.

Reward commands remain supported and run once per selected reward after the native items commit. Commands and event listeners belong to other plugins: their delivered items cannot be predicted for the inventory check, and their effects cannot be rolled back. If a command fails, do not retry the entire redemption automatically. Later rewards still receive their callback attempt. Thrown callback exceptions are logged independently, so a command or firework failure cannot suppress the receipt event for delivered items; command dispatch otherwise retains its existing error handling. Server crashes between file persistence and player saving are not a durable cross-file transaction.

## Configuration

### Basic Setup

```yaml
Crate:
  CrateType: SelectCrate
  CrateName: '&6&lSelect Crate'
  RequiredKeys: 1
  
  SelectCrate:
    MaxSelections: 5  # Maximum distinct rewards per confirmation; minimum 1.
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
    
    BlockedPrize:
      Material: STRUCTURE_VOID
      Name: '&cPrêmio indisponível'
      Lore:
        - '&7Você não pode selecionar este prêmio.'

    Messages:
      NoSelection: '&cSelecione ao menos um produto antes de confirmar!'
      Counter: '&eSelecionados: %selected%/%max% | Chaves: %cost%/%keys%'
      Cancel: '&cCancelar'
      SelectionLimit: '&cVocê não possui chaves suficientes para esta seleção. Desmarque outro produto.'
      Unavailable: '&cSaldo insuficiente. Desmarque outro produto.'
      PrizeCost: '&7Custo: %cost% chave(s).'
      MaxSelections: '&cVocê pode selecionar no máximo %max% prêmios.'
      BlockedPrize: '&cVocê não pode selecionar este prêmio.'
      InventoryFull: '&cVocê não possui espaço suficiente no inventário para receber todos os produtos selecionados.'
      InvalidReward: '&cNão foi possível preparar os produtos. Avise a equipe.'
```

The Counter message supports `%selected%` (number of rewards), `%max%` (configured selection cap), `%cost%` (combined cost) and `%keys%` (available keys). `%limit%` remains accepted as an alias for the available key budget. Update existing custom Counter messages to use the cost placeholders when mixing prices. The cap defaults to 5 even in existing configurations that omit MaxSelections. Values below 1 use 1. Lowering the cap while a menu is open rejects confirmation until the player deselects enough rewards; nothing is charged or delivered. Explicit free openings still allow one choice and charge no keys.

### Prize Configuration

Prizes in SelectCrate work the same as other crate types, but MaxRange and Chance are not used since all prizes are available for selection:

```yaml
  Prizes:
    1:
      RequiredKeys: 2  # Overrides Crate.RequiredKeys, even if it is higher or lower.
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

## Blacklisted rewards

Add permissions to each prize's `BlackListed-Permissions` list. If the player has any listed permission, SelectCrate replaces that prize's icon with `Crate.SelectCrate.BlockedPrize` and rejects selection. The default icon is `STRUCTURE_VOID`; its material, name and lore are configurable. `Crate.SelectCrate.Messages.BlockedPrize` configures the click/confirmation warning.

```yaml
Crate:
  Prizes:
    1:
      BlackListed-Permissions:
        - crazycrates.claimed.select_sword
```

This list checks permissions; it does not grant them. To make a reward claimable once, configure your permission/reward setup to grant the listed permission after the claim. Existing blacklist behavior is preserved: OP players bypass it. Other crate types keep their existing alternative-reward behavior.

Permissions are rechecked when clicking and confirming, including after item preparation. Selecting or navigating removes newly blocked choices first, releasing selection capacity and updating the counter. Only one confirmation task may be pending per session; after a rejected batch, redeeming the remaining choices requires a new confirmation. If a selected reward becomes blocked, the whole confirmation is rejected without consuming keys or delivering any rewards, and blocked choices are removed so the player can choose replacements.

## Important Notes

1. **GUI Size**: Reserve the bottom row (slots 45-53 in a 54-slot GUI) for controls. Prizes that do not fit are split into pages. Controls outside the reserved row or overlapping another control fall back to separate slots. A 9-slot menu has one prize slot.

2. **Required Keys**: A prize-level integer overrides `Crate.RequiredKeys`; omitting the field inherits the crate value. Zero and negative values retain the existing minimum of one key. Physical SelectCrate menus can open with a single accepted key, so cheaper overrides remain reachable; exact costs are enforced on selection and confirmation. Other crate types retain their existing opening checks.

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
