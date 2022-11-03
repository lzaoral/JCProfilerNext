package jcprofiler.args.converters;

import com.beust.jcommander.converters.EnumConverter;
import jcprofiler.util.enums.InputDivision;

/**
 * Parameter converter for the {@link InputDivision} enum
 */
public class InputDivisionConverter extends EnumConverter<InputDivision> {
    public InputDivisionConverter(final String optionName, final Class<InputDivision> clazz) {
        super(optionName, clazz);
    }
}
