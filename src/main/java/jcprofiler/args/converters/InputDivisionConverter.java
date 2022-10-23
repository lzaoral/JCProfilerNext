package jcprofiler.args.converters;

import com.beust.jcommander.converters.EnumConverter;
import jcprofiler.util.enums.InputDivision;

public class InputDivisionConverter extends EnumConverter<InputDivision> {
    /**
     * Constructs a new converter.
     *
     * @param optionName the option name for error reporting
     * @param clazz      the enum class
     */
    public InputDivisionConverter(final String optionName, final Class<InputDivision> clazz) {
        super(optionName, clazz);
    }
}
