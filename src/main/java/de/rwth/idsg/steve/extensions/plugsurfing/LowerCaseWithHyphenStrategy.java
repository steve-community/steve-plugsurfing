package de.rwth.idsg.steve.extensions.plugsurfing;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;

/**
 * @author Sevket Goekay <goekay@dbis.rwth-aachen.de>
 * @since 02.09.2015
 */
public class LowerCaseWithHyphenStrategy extends PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy {

    private static final char HYPHEN = '-';

    /**
     * The same code as in the overridden method. The only change is from underscore to hyphen.
     */
    @Override
    public String translate(String input) {
        if (input == null) return input; // garbage in, garbage out
        int length = input.length();
        StringBuilder result = new StringBuilder(length * 2);
        int resultLength = 0;
        boolean wasPrevTranslated = false;
        for (int i = 0; i < length; i++)
        {
            char c = input.charAt(i);
            if (i > 0 || c != '_') // skip first starting underscore
            {
                if (Character.isUpperCase(c) || Character.isDigit(c))
                {
                    if (!wasPrevTranslated && resultLength > 0 && result.charAt(resultLength - 1) != '_')
                    {
                        result.append(HYPHEN);
                        resultLength++;
                    }
                    c = Character.toLowerCase(c);
                    wasPrevTranslated = true;
                }
                else
                {
                    wasPrevTranslated = false;
                }
                result.append(c);
                resultLength++;
            }
        }
        return resultLength > 0 ? result.toString() : input;
    }
}
