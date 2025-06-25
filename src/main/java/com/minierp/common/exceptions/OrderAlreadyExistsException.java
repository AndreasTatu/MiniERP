package com.minierp.common.exceptions;

public class OrderAlreadyExistsException extends RuntimeException {
  public OrderAlreadyExistsException(String message) {
    super(message);
  }
}
