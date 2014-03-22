package com.kaazing.nuklei;

/**
 * Basic interface for a running service, aka Nukleus.
 */
public interface Nukleus
{
    /**
     * Start nukleus
     */
    public void start();

    /**
     * Process a message
     */
    public void process();

    /**
     * Close down nukleus
     */
    public void close();
}
