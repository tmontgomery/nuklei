package com.kaazing.nuklei;

/**
 * Basic interface for a running service, aka Nukleus.
 */
public interface Nukleus
{
    /**
     * Start nukleus
     */
    void start();

    /**
     * Process a message
     */
    void process();

    /**
     * Close down nukleus
     */
    void close();
}
