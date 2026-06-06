package com.cueb.demo.classroom.context;

import com.cueb.demo.classroom.vo.ClassroomStatusVO;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.util.List;

/**
 * 请求级别的数据桥梁。
 * ClassroomAgentServiceV2 在工具方法中将 VO 写入此处，
 * Controller 在 ChatClient 调用结束后取出，返回给前端。
 */
@Component
@RequestScope
public class ClassroomQueryContext {

    private List<ClassroomStatusVO> lastQueryResult;
    private String lastReportResult;

    public List<ClassroomStatusVO> getLastQueryResult() {
        return lastQueryResult;
    }

    public void setLastQueryResult(List<ClassroomStatusVO> lastQueryResult) {
        this.lastQueryResult = lastQueryResult;
    }

    public String getLastReportResult() {
        return lastReportResult;
    }

    public void setLastReportResult(String lastReportResult) {
        this.lastReportResult = lastReportResult;
    }

    /** 每次请求结束后可调用，防止状态残留 */
    public void clear() {
        this.lastQueryResult = null;
        this.lastReportResult = null;
    }
}
