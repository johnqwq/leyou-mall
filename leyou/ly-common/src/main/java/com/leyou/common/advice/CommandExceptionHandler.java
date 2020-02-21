package com.leyou.common.advice;

import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.ExceptionResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 通用异常处理
 */
@ControllerAdvice // 启动通用异常处理
public class CommandExceptionHandler {
    @ExceptionHandler(LyException.class) // 指定通用异常处理的异常对象
    // 如果这里只是单纯地返回状态码和字符串，则可以用ResponseEntity<String>
    public ResponseEntity<ExceptionResult> handleException(LyException e) {
        // 为了将body里单纯的传字符串变为传一个高级的参数列表，我们选择创建一个对象承载信息，里面有状态码、错误信息和时间戳
        return ResponseEntity.status(e.getExceptionEnum().getCode()).body(new ExceptionResult(e.getExceptionEnum()));
    }
}
