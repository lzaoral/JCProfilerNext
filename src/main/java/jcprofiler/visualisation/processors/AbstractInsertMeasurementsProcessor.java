// SPDX-FileCopyrightText: 2022-2024 Lukáš Zaoral <lukaszaoral@outlook.com>
// SPDX-License-Identifier: GPL-3.0-only

package jcprofiler.visualisation.processors;

import jcprofiler.args.Args;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.reference.CtExecutableReference;

import java.util.List;
import java.util.Map;

/**
 * General class for annotation of profiled sources
 */
public abstract class AbstractInsertMeasurementsProcessor extends AbstractProcessor<CtInvocation<Void>> {
    /**
     * Commandline arguments
     */
    protected final Args args;
    /**
     * Map between traps and measurements
     */
    protected final Map<String, List<Long>> measurements;

    private static final Logger log = LoggerFactory.getLogger(AbstractInsertMeasurementsProcessor.class);

    /**
     * Constructs the {@link AbstractInsertMeasurementsProcessor} class.
     *
     * @param args         object with commandline arguments
     * @param measurements map between traps and measurements
     */
    protected AbstractInsertMeasurementsProcessor(final Args args, final Map<String, List<Long>> measurements) {
        this.args = args;
        this.measurements = measurements;
    }

    /**
     * Decides whether the input {@link CtInvocation} corresponds to
     * a {@code PM#check(short)} call.
     *
     * @param  statement the candidate invocation
     * @return           true if yes, otherwise false
     */
    @Override
    public boolean isToBeProcessed(final CtInvocation<Void> statement) {
        final CtExecutableReference<?> executable = statement.getExecutable();
        return executable.getDeclaringType().getSimpleName().equals("PM") &&
               executable.getSignature().equals("check(short)") &&
               executable.getType().equals(getFactory().Type().voidPrimitiveType()) &&
               executable.isStatic();
    }

    /**
     * Returns a commentary contents to replace the {@code PM#check(short)} call
     * for given performance trap.
     *
     * @param  fieldName name of the performance trap field
     * @return           comment annotation contents
     */
    protected abstract String getCommentString(final String fieldName);

    /**
     * Replaces the {@code PM#check(short)} calls with
     * a commentary with measurement statistics.
     *
     * @param invocation invocation to be processed
     */
    @Override
    public void process(final CtInvocation<Void> invocation) {
        final CtFieldRead<?> trapFieldRead = (CtFieldRead<?>) invocation.getArguments().get(0);
        final String fieldName = trapFieldRead.getVariable().getSimpleName();

        // skip if this trap was not measured
        if (!measurements.containsKey(fieldName))
            return;

        final String commentContents = getCommentString(fieldName);

        log.debug("Inserting comment with measurements at {}.", invocation.getPosition());
        invocation.replace(getFactory().createInlineComment(commentContents));
    }
}
