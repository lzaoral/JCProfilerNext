package jcprofiler.args.converters;

import com.beust.jcommander.converters.EnumConverter;
import jcprofiler.util.enums.Mode;

public class ModeConverter extends EnumConverter<Mode> {
    /**
     * Constructs a new converter.
     *
     * @param optionName the option name for error reporting
     * @param clazz      the enum class
     */
    public ModeConverter(String optionName, Class<Mode> clazz) {
        super(optionName, clazz);
    }
}
