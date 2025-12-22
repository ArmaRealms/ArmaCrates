# SelectCrate - User Selection Crate Type

## Overview
SelectCrate is a new crate type for CrazyCrates that allows players to **choose** which prize they want to receive instead of relying on random chance.

## How It Works

1. **Opening the Crate**: When a player opens a SelectCrate (by right-clicking a physical crate or using `/crates open <crate>`), they are presented with a GUI showing all available prizes.

2. **Selecting a Prize**: Players click on the prize they want to receive. The selected item will have:
   - An enchantment glow effect
   - A special marker in the lore indicating it's selected

3. **Confirming**: After selecting a prize, the player clicks the confirmation button (default: green concrete in slot 49).

4. **Receiving the Prize**: Upon confirmation:
   - The plugin validates the player still has the required keys
   - Keys are removed from the player's inventory
   - The selected prize is given to the player
   - The GUI closes automatically

## Key Features

- **Key Validation**: Keys are checked when opening AND when confirming to prevent exploits
- **Key Consumption**: Keys are only consumed when the player **confirms** their selection, not when opening the GUI
- **Visual Feedback**: Selected items have a glow effect and special lore
- **Configurable**: GUI size, button placement, messages, and marker appearance are all configurable
- **Safe**: Players can close the GUI without confirming to cancel the operation (no keys consumed)

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
      Slot: 49  # Bottom right of 6-row inventory
      Item:
        Material: LIME_CONCRETE
        Name: '&aConfirm Choice'
        Lore:
          - '&7Click to receive the selected prize.'
    
    SelectionMarker:
      Material: NETHER_STAR
      Name: '&e&l✓ SELECTED'
      Lore:
        - '&7This prize is selected.'
    
    Messages:
      NoSelection: '&cPlease select a prize before confirming!'
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

1. **GUI Size**: The GUI size must be large enough to display all your prizes plus the confirmation button. Reserve the bottom row (slots 45-53) for control buttons.

2. **Required Keys**: While you can set `RequiredKeys` to any value, SelectCrate will consume keys based on this setting when the player confirms.

3. **No Random Selection**: Unlike other crate types, **all prizes are available** to choose from. The Chance and MaxRange values are ignored.

4. **Session Safety**: If a player disconnects or the server restarts while they have a SelectCrate open, no keys are consumed and the session is cleaned up automatically.

5. **Inventory Protection**: Players cannot move items in the SelectCrate GUI. All inventory interactions except selection are blocked.

## Example Use Cases

- **VIP Reward Crates**: Let VIP players choose their monthly reward
- **Event Prizes**: Allow event winners to select their preferred prize
- **Rank Kits**: Let players pick their starting kit when ranking up
- **Donation Rewards**: Give donors choice in what they receive

## Compatibility

SelectCrate is compatible with all CrazyCrates features:
- Physical and virtual keys
- PlaceholderAPI placeholders
- Commands as rewards
- Multiple items per prize
- Custom messages
- Holograms
- Preview GUI

## See Also

- Full example configuration: `SelectCrateExample.yml`
- CrazyCrates Wiki: https://docs.crazycrew.us/crazycrates/home
