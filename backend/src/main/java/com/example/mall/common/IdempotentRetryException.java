package com.example.mall.common;

/**
 * 幂等冲突信号：唯一索引 uk_request_id 拦下重复插入时抛出。
 * 抛出异常是为了让 @Transactional 回滚已经扣掉的库存，
 * 再由 Controller 在新事务里查出已落库的订单返回。
 */
public class IdempotentRetryException extends RuntimeException {

    private final String requestId;

    public IdempotentRetryException(String requestId) {
        super("重复请求: " + requestId);
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }
}
