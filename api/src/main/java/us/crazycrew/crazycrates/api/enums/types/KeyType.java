package us.crazycrew.crazycrates.api.enums.types;

/**
 * An enum class that contains all our current key types.
 *
 * @author Ryder Belserion
 * @version 0.3
 */
public enum KeyType {

    /**
     * The physical key type.
     */
    physical_key("physical_key"),
    /**
     * The virtual key type.
     */
    virtual_key("virtual_key"),
    /**
     * The free key type.
     */
    free_key("free_key");

    private final String name;

    /**
     * A constructor to build a key type reference.
     *
     * @param name of the key-type
     */
    KeyType(final String name) {
        this.name = name;
    }

    /**
     * Get the key type by name.
     *
     * @param type the name of the key-type
     * @return the type of key otherwise null
     */
    public static KeyType getFromName(final String type) {
        if (type.equalsIgnoreCase("virtual") || type.equalsIgnoreCase("v")) {
            return KeyType.virtual_key;
        } else if (type.equalsIgnoreCase("physical") || type.equalsIgnoreCase("p")) {
            return KeyType.physical_key;
        } else if (type.equalsIgnoreCase("free") || type.equalsIgnoreCase("f")) {
            return KeyType.free_key;
        }

        return null;
    }

    /**
     * Get the name of the key type.
     *
     * @return the name of the key-type
     */
    public String getName() {
        return this.name;
    }
}