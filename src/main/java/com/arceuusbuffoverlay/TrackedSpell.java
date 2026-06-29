package com.arceuusbuffoverlay;

enum TrackedSpell
{
    GREATER_CORRUPTION("Greater Corruption"),
    DEATH_CHARGE("Death Charge"),
    MARK_OF_DARKNESS("Mark of Darkness");

    private final String displayName;

    TrackedSpell(String displayName)
    {
        this.displayName = displayName;
    }

    String getDisplayName()
    {
        return displayName;
    }

    static TrackedSpell fromText(String text)
    {
        if (text == null)
        {
            return null;
        }

        String lower = stripTags(text).toLowerCase();

        for (TrackedSpell spell : values())
        {
            if (lower.contains(spell.displayName.toLowerCase()))
            {
                return spell;
            }
        }

        return null;
    }

    static String stripTags(String text)
    {
        if (text == null)
        {
            return "";
        }

        return text.replaceAll("<[^>]*>", "");
    }
}
