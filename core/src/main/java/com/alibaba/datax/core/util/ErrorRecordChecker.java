package com.alibaba.datax.core.util;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.core.statistics.communication.Communication;
import com.alibaba.datax.core.statistics.communication.CommunicationTool;
import com.alibaba.datax.core.util.container.CoreConstant;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ErrorRecordChecker 错误记录检查者
 *
 * ErrorRecordChecker是一个监控类，主要用来检查任务是否到达错误记录限制。
 *
 * 检查任务是否到达错误记录限制。有检查条数（recordLimit）和百分比(percentageLimit)两种方式。
 * 1. errorRecord表示出错条数不能大于限制数，当超过时任务失败。比如errorRecord为0表示不容许任何脏数据。
 * 2. errorPercentage表示出错比例，在任务结束时校验。
 * 3. errorRecord优先级高于errorPercentage。
 *
 */
public final class ErrorRecordChecker {
    private static final Logger LOG = LoggerFactory
            .getLogger(ErrorRecordChecker.class);

    private Long recordLimit;
    private Double percentageLimit;

    /**
     * ① 构造函数ErrorRecordChecker(Configuration configuration)：
     * 主要就是从任务配置文件job.json里面获取errorLimit.record错误记录数限制及errorLimit.percentage错误记录百分比的值：
     * @param configuration
     */
    public ErrorRecordChecker(Configuration configuration) {
        this(configuration.getLong(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT_RECORD),
                configuration.getDouble(CoreConstant.DATAX_JOB_SETTING_ERRORLIMIT_PERCENT));
    }

    public ErrorRecordChecker(Long rec, Double percentage) {
        recordLimit = rec;
        percentageLimit = percentage;

        if (percentageLimit != null) {
            Validate.isTrue(0.0 <= percentageLimit && percentageLimit <= 1.0,
                    "脏数据百分比限制应该在[0.0, 1.0]之间");
        }

        if (recordLimit != null) {
            Validate.isTrue(recordLimit >= 0,
                    "脏数据条数现在应该为非负整数");

            // errorRecord优先级高于errorPercentage.
            percentageLimit = null;
        }
    }

    /**
     * ② 检查错误记录数限制checkRecordLimit(Communication communication)：
     * 主要就是从communication里获取总共的错误记录数，然后判断是否超出配置的值，如果是，则抛出异常
     * @param communication
     */
    public void checkRecordLimit(Communication communication) {
        if (recordLimit == null) {
            return;
        }

        long errorNumber = CommunicationTool.getTotalErrorRecords(communication);
        if (recordLimit < errorNumber) {
            LOG.debug(
                    String.format("Error-limit set to %d, error count check.",
                            recordLimit));
            throw DataXException.asDataXException(
                    FrameworkErrorCode.PLUGIN_DIRTY_DATA_LIMIT_EXCEED,
                    String.format("脏数据条数检查不通过，限制是[%d]条，但实际上捕获了[%d]条.",
                            recordLimit, errorNumber));
        }
    }

    /**
     * ③ 检查错误记录百分比checkPercentageLimit(Communication communication)：
     * 主要就是从communication里获取总共的错误记录数与总数的百分比值，然后判断是否超出配置的值，如果是，则抛出异常：
     * @param communication
     */
    public void checkPercentageLimit(Communication communication) {
        if (percentageLimit == null) {
            return;
        }
        LOG.debug(String.format(
                "Error-limit set to %f, error percent check.", percentageLimit));

        long total = CommunicationTool.getTotalReadRecords(communication);
        long error = CommunicationTool.getTotalErrorRecords(communication);

        if (total > 0 && ((double) error / (double) total) > percentageLimit) {
            throw DataXException.asDataXException(
                    FrameworkErrorCode.PLUGIN_DIRTY_DATA_LIMIT_EXCEED,
                    String.format("脏数据百分比检查不通过，限制是[%f]，但实际上捕获到[%f].",
                            percentageLimit, ((double) error / (double) total)));
        }
    }
}
