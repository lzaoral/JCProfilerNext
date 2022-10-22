package jcprofiler.args.converters;

import com.beust.jcommander.converters.EnumConverter;
import jcprofiler.util.enums.TimeUnit;

public class TimeUnitConverter extends EnumConverter<TimeUnit> {
    /**
     * Constructs a new converter.
     *
     * @param optionName the option name for error reporting
     * @param clazz      the enum class
     */
    public TimeUnitConverter(String optionName, Class<TimeUnit> clazz) {
        super(optionName, clazz);
    }
}
