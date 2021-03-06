package net.tomp2p.futures;

import java.io.IOException;

import net.tomp2p.message.Buffer;


public class FutureDirect extends FutureWrapper2<FutureDirect, FutureResponse> {
    
    private final FutureResponse futureResponse;
    
    public FutureDirect(String failed) {
    	super(new FutureResponse(null));
    	self(this);
    	this.futureResponse = wrappedFuture();
    	futureResponse.setFailed(failed);
    	setFailed(failed);
    }
    
    public FutureDirect(FutureResponse futureResponse) {
    	super(futureResponse);
    	self(this);
        this.futureResponse = futureResponse;
        waitFor();
    }
    
    public Buffer getBuffer() {
        synchronized (lock) {
            return futureResponse.getResponse().getBuffer(0);
        }
    }
    
    public Object object() throws ClassNotFoundException, IOException {
        synchronized (lock) {
            return getBuffer().object();
        }
    }

}
