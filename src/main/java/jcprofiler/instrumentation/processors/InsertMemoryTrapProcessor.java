// SPDX-FileCopyrightText: 2022 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.instrumentation.processors;

import jcprofiler.args.Args;

import pro.javacard.JavaCardSDK;

import spoon.reflect.code.CtLiteral;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.reference.CtTypeReference;

/**
 * Class for performance trap insertion in memory mode
 * <br>
 * Applicable to instances of {@link CtExecutable}.
 */
public class InsertMemoryTrapProcessor extends AbstractInsertTrapProcessor<CtExecutable<?>> {
    /**
     * Constructs the {@link InsertMemoryTrapProcessor} class.
     *
     * @param args object with commandline arguments
     */
    public InsertMemoryTrapProcessor(final Args args) {
        super(args);
    }

    /**
     * Inserts traps into the given {@link CtExecutable} instance.
     *
     * @param executable an executable instance
     */
    @Override
    public void process(final CtExecutable<?> executable) {
        super.process(executable);
        fixPMArrayLength();
    }

    /**
     * Sets the value of {@code PM#ARRAY_LENGTH} to the expected number of bytes
     * needed for memory profiling of given executable.
     *
     * @throws RuntimeException when the PM does not contain the {@code PM#ARRAY_LENGTH} field.
     */
    private void fixPMArrayLength() {
        // handle support for 16bit and 32bit values
        final int arrayLength = trapCount *
                (!args.useSimulator && args.jcSDK.getVersion().ordinal() >= JavaCardSDK.Version.V304.ordinal()
                    ? Integer.BYTES
                    : Short.BYTES);

        final CtTypeReference<Short> shortRef = getFactory().Type().shortPrimitiveType();
        final CtLiteral<Integer> arrayLengthLiteral = getFactory().createLiteral(arrayLength);
        arrayLengthLiteral.addTypeCast(shortRef);

        // get PM.ARRAY_LENGTH field
        final CtField<?> arrayLengthField = PM.getField("ARRAY_LENGTH");

        if (arrayLengthField == null)
            throw new RuntimeException("PM does not contain an ARRAY_LENGTH field.");
        if (!arrayLengthField.getType().equals(shortRef))
            throw new RuntimeException(
                    "PM.ARRAY_LENGTH field is of type " + arrayLengthField.getType() + "! Expected short.");

        @SuppressWarnings("unchecked") // the runtime check is above
        final CtField<Short> arrayLengthFieldCasted = (CtField<Short>) arrayLengthField;

        @SuppressWarnings("unchecked")
        // Unfortunately, this is the best solution we have since Spoon does not reflect type casts in type parameters.
        final CtLiteral<Short> arrayLengthLiteralCasted = (CtLiteral<Short>) (Object) arrayLengthLiteral;
        arrayLengthFieldCasted.setAssignment(arrayLengthLiteralCasted);
    }
}
