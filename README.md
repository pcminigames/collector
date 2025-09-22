

# Collector

A Minecraft minigame with two modes:

- **Item Mode:** Try to collect as many unique items as possible within the time limit.
- **Deaths Mode:** Try to get as many deaths as possible within the time limit.


Choose your strategy based on the selected mode!

**Requires [pythoncraft's GameLib](https://github.com/pcminigames/gamelib) plugin to run.**


## Installation

1. Make sure you are running a compatible Minecraft server (for Collector v2.0, use Paper 1.21.8 or newer).
2. Download the latest release from the [releases page](https://github.com/pcminigames/collector/releases).
3. Download the [GameLib](https://github.com/pcminigames/gamelib/releases) plugin.
4. Put both `.jar` files in your server's `plugins` folder.
5. Start or restart your server.



## Usage

1. First, you need to decide which mode to play: **Item Mode** or **Death Mode**.
2. You can start the game by running `/collector items <seconds>` for Item Mode or `/collector deaths <seconds>` for Death Mode. The `<seconds>` parameter is optional and sets the duration of the game - default is 900 seconds (15 minutes).
3. You earn points by collecting unique items or dying in unique ways, depending on the mode. You can see your current score on the sidebar.
4. When the time runs out, the game ends and the winner is announced based on the points.

### Scoring

In **Item Mode**, your goal is to collect as many unique items as possible before the timer runs out. The player with the most unique items wins. Items are considered unique based on their item type, not their special properties, like enchantments or custom names.

In **Death Mode**, your goal is to get as many unique deaths as possible before the timer runs out. The player with the most deaths wins. The deaths are compared based on the death message, which means that `Name fell from a high place` and `Name fell from a high place while trying to escape Zombie` are considered different deaths.


## Notes

- Make sure to try out other [pythoncraft's minigames](https://github.com/orgs/pcminigames/repositories).
- Feel free to suggest improvements or report issues ([here](https://github.com/pcminigames/item-collector/issues)).

## Issues

If you find any issues or have suggestions for improvements, please report them on the [issues page](https://github.com/pcminigames/item-collector/issues).
