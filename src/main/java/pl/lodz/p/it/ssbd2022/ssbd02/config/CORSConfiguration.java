package pl.lodz.p.it.ssbd2022.ssbd02.config;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

@Provider
public class CORSConfiguration implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        containerResponseContext.getHeaders().add("Access-Control-Allow-Origin", "https://studapp.it.p.lodz.pl:8402");
        containerResponseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, PUT, POST, DELETE");
        containerResponseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        containerResponseContext.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, if-match");
        containerResponseContext.getHeaders().add("Access-Control-Expose-Headers", "etag");
        containerResponseContext.getHeaders().add("Access-Control-Max-Age", "10");
    }
}
