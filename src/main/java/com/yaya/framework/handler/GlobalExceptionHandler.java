package com.yaya.framework.handler;

import cn.hutool.core.collection.CollectionUtil;
import com.yaya.framework.dto.Result;
import com.yaya.framework.enums.HttpStatus;
import com.yaya.framework.exception.GlobalCommonException;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局统一异常处理: 将各类异常统一转换为 Result 返回
 * <p>
 * 注意: @Hidden 用于阻止 springdoc 扫描本 advice(knife4j 底层 springdoc 2.3.0 扫描 advice 时
 * 会触发 NoSuchMethodError 导致在线文档 500); 后续新增其他 @RestControllerAdvice 类时
 * 必须同样标注 @Hidden, 否则 /v3/api-docs 会报 500
 *
 * @author yaya
 */
@Slf4j
@RestControllerAdvice
@Hidden
public class GlobalExceptionHandler {

    /**
     * 业务异常: 按异常携带的状态码与提示返回
     */
    @ExceptionHandler(GlobalCommonException.class)
    public Result<Void> handleGlobalCommonException(GlobalCommonException e) {
        log.error("业务异常: {}", e.getMessage());
        return new Result<>(e.getCode(), e.getMessage());
    }

    /**
     * 请求参数缺失: 未传 @RequestParam 指定的必要参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParameter(MissingServletRequestParameterException e) {
        log.error("请求参数缺失: {}", e.getMessage());
        return new Result<>(HttpStatus.BAD_REQUEST.getCode(), "缺少必要参数: " + e.getParameterName());
    }

    /**
     * 请求参数类型错误: 如 Long 型参数传入了非数字
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.error("请求参数类型错误: {}", e.getMessage());
        return new Result<>(HttpStatus.BAD_REQUEST.getCode(), "参数类型错误: " + e.getName());
    }

    /**
     * 上传文件大小超过 yml 中 spring.servlet.multipart 配置的上限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.error("上传文件大小超限: {}", e.getMessage());
        return new Result<>(HttpStatus.BAD_REQUEST.getCode(), "上传文件大小超过限制");
    }

    /**
     * @ @Valid 参数验证的异常处理
     * @param e 异常
     * @return 提示消息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentNotValidException e) {
        log.error("参数验证的异常: {}", e.getMessage());
        return new Result<>(HttpStatus.BAD_REQUEST.getCode(), CollectionUtil.isEmpty(e.getBindingResult().getFieldErrors())?"请求参数不能为空": e.getBindingResult().getFieldErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining(",")));
    }

    /**
     * 权限不足: @PreAuthorize 等方法级权限校验拒绝 <br/>
     * 注意: MVC 异常解析先于 Security 过滤器执行, 不单独处理会被下方兜底分支转成"系统异常"
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限不足: {}", e.getMessage());
        return Result.error(HttpStatus.FORBIDDEN);
    }

    /**
     * 资源不存在: 未映射的路径(含生产环境已关闭的 /v3/api-docs)返回 404 <br/>
     * 不单独处理会落入兜底分支, 被当作系统异常记录堆栈并返回 200+错误码
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("资源不存在: {}", e.getResourcePath());
        return Result.error(HttpStatus.NOT_FOUND);
    }

    /**
     * 系统异常: 兜底处理(记录完整堆栈, 返回统一提示)
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(HttpStatus.ERROR);
    }
}
