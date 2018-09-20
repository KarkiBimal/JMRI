package jmri.util.managers;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jmri.jmrit.logix.Warrant;
import jmri.jmrit.logix.WarrantManager;

/**
 * This manager always throws an IllegalArgumentException from provide and get methods.
 * It is used for testing exception handling in tests.
 */
public class WarrantManagerThrowException extends WarrantManager {

    public WarrantManagerThrowException() {
        super();
    }
    
    /** {@inheritDoc} */
    @Override
    public Warrant createNewWarrant(String systemName, String userName, boolean SCWa, long TTP) {
        throw new IllegalArgumentException("Illegal argument");
    }
    
    /** {@inheritDoc} */
    @Override
    public Warrant provideWarrant(@Nonnull String name) {
        throw new IllegalArgumentException("Illegal argument");
    }
    
    /** {@inheritDoc} */
    @Override
    public Warrant getWarrant(@Nonnull String name) {
        throw new IllegalArgumentException("Illegal argument");
    }
    
    /** {@inheritDoc} */
    @Override
    public Warrant getBySystemName(@Nonnull String name) {
        throw new IllegalArgumentException("Illegal argument");
    }
    
    /** {@inheritDoc} */
    @Override
    public Warrant getByUserName(String key) {
        throw new IllegalArgumentException("Illegal argument");
    }
    
}
