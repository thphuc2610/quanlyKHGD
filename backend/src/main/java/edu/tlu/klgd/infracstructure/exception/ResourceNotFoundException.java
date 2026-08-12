package edu.tlu.klgd.infracstructure.exception;

import edu.tlu.klgd.domain.common.ApiMessage;
import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resourceName, Object id) {
        super(HttpStatus.NOT_FOUND, String.format(ApiMessage.RESOURCE_NOT_FOUND_FORMAT, resourceName, id));
    }
}
