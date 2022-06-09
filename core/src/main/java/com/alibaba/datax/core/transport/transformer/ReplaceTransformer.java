package com.alibaba.datax.core.transport.transformer;

import com.alibaba.datax.common.element.Column;
import com.alibaba.datax.common.element.Record;
import com.alibaba.datax.common.element.StringColumn;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.transformer.Transformer;

import java.util.Arrays;

/**
 * no comments.
 * Created by liqiang on 16/3/4.
 */
public class ReplaceTransformer extends Transformer {
    //设置Transformer名字，便于配置
    public ReplaceTransformer() {
        setTransformerName("dx_replace");
    }

    /**
     * 入参的record表示源数据，返回的是处理后的record
     *
     * 调用流程：
     * Transformer.evaluate()
     * <=
     * ComplexTransformer.evaluate()
     * <=
     * TransformerExchanger.doTransformer()
     * <=
     * RecordSender.sendToWriter()
     * <=
     * Reader.Task.startRead()
     *
     *
     *翻译上面的流程，用人话说就是（这里改为了正向流程了）：
     * Reader读内容（`Reader.Task.startRead()`）
     * =>
     * Reader读内容的方法最后一步调用发送给Writer（`RecordSender.sendToWriter() `）
     * =>
     * 发送之前使用Exchanger交换机来进行transform操作（`TransformerExchanger.doTransformer() `）
     * =>
     * transform会触发evaluate去处理业务（`ComplexTransformer.evaluate()`）
     * =>
     * 最后处理发送到writer前的业务操作（`Transformer.evaluate() `）
     */
    @Override
    public Record evaluate(Record record, Object... paras) {

        int columnIndex;
        int startIndex;
        int length;
        String replaceString;
        // 以下开始处理数据，这里做的是替换的操作
        try {
            if (paras.length != 4) {
                throw new RuntimeException("dx_replace paras must be 4");
            }

            columnIndex = (Integer) paras[0];
            startIndex = Integer.valueOf((String) paras[1]);
            length = Integer.valueOf((String) paras[2]);
            replaceString = (String) paras[3];
        } catch (Exception e) {
            throw DataXException.asDataXException(TransformerErrorCode.TRANSFORMER_ILLEGAL_PARAMETER, "paras:" + Arrays.asList(paras).toString() + " => " + e.getMessage());
        }

        Column column = record.getColumn(columnIndex);

        try {
            String oriValue = column.asString();

            //如果字段为空，跳过replace处理
            if(oriValue == null){
                return  record;
            }
            String newValue;
            if (startIndex > oriValue.length()) {
                throw new RuntimeException(String.format("dx_replace startIndex(%s) out of range(%s)", startIndex, oriValue.length()));
            }
            if (startIndex + length >= oriValue.length()) {
                newValue = oriValue.substring(0, startIndex) + replaceString;
            } else {
                newValue = oriValue.substring(0, startIndex) + replaceString + oriValue.substring(startIndex + length, oriValue.length());
            }

            record.setColumn(columnIndex, new StringColumn(newValue));

        } catch (Exception e) {
            throw DataXException.asDataXException(TransformerErrorCode.TRANSFORMER_RUN_EXCEPTION, e.getMessage(),e);
        }
        return record;
    }
}
