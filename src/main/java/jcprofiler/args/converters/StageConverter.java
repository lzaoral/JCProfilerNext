package jcprofiler.args.converters;

import com.beust.jcommander.converters.EnumConverter;
import jcprofiler.util.Stage;

public class StageConverter extends EnumConverter<Stage> {
    /**
     * Constructs a new converter.
     *
     * @param optionName the option name for error reporting
     * @param clazz      the enum class
     */
    public StageConverter(String optionName, Class<Stage> clazz) {
        super(optionName, clazz);
    }
}
