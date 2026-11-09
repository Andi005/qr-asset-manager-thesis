package io.github.andi005.qram.common;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Something that was asked for by id or tag does not exist. Shown as a 404 page. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

  public NotFoundException(String what) {
    super(what + " not found");
  }
}
