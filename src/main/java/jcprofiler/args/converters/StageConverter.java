package jcprofiler.args.converters;

import com.beust.jcommander.converters.EnumConverter;
import jcprofiler.util.enums.Stage;

/**
 * Parameter converter for the {@link Stage} enum
 */
public class StageConverter extends EnumConverter<Stage> {
    public StageConverter(String optionName, Class<Stage> clazz) {
        super(optionName, clazz);
    }
}
