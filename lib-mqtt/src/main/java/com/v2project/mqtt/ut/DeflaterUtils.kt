package com.v2project.mqtt.ut

import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * DeflaterUtils 压缩字符串
 */
object DeflaterUtils {

    /**
     * 压缩
     */
    fun compress(unzipString: String): ByteArray {
        if (unzipString.isEmpty()) {
            return byteArrayOf()
        }
        /**
         * https://www.yiibai.com/javazip/javazip_deflater.html#article-start
         * 0 ~ 9 压缩等级 低到高
         * public static final int BEST_COMPRESSION = 9;            最佳压缩的压缩级别。
         * public static final int BEST_SPEED = 1;                  压缩级别最快的压缩。
         * public static final int DEFAULT_COMPRESSION = -1;        默认压缩级别。
         * public static final int DEFAULT_STRATEGY = 0;            默认压缩策略。
         * public static final int DEFLATED = 8;                    压缩算法的压缩方法(目前唯一支持的压缩方法)。
         * public static final int FILTERED = 1;                    压缩策略最适用于大部分数值较小且数据分布随机分布的数据。
         * public static final int FULL_FLUSH = 3;                  压缩刷新模式，用于清除所有待处理的输出并重置拆卸器。
         * public static final int HUFFMAN_ONLY = 2;                仅用于霍夫曼编码的压缩策略。
         * public static final int NO_COMPRESSION = 0;              不压缩的压缩级别。
         * public static final int NO_FLUSH = 0;                    用于实现最佳压缩结果的压缩刷新模式。
         * public static final int SYNC_FLUSH = 2;                  用于清除所有未决输出的压缩刷新模式; 可能会降低某些压缩算法的压缩率。
         */
        //使用指定的压缩级别创建一个新的压缩器。
        val deflater = Deflater(Deflater.BEST_COMPRESSION)
        //设置压缩输入数据。
        deflater.setInput(unzipString.toByteArray())
        //当被调用时，表示压缩应该以输入缓冲区的当前内容结束。
        deflater.finish()
        val bytes = ByteArray(2048)
        val outputStream = ByteArrayOutputStream(2048)
        while (!deflater.finished()) {
            //压缩输入数据并用压缩数据填充指定的缓冲区。
            val length = deflater.deflate(bytes)
            outputStream.write(bytes, 0, length)
        }
        //关闭压缩器并丢弃任何未处理的输入。
        deflater.end()
        return outputStream.toByteArray()
    }

    /**
     * 解压缩
     */
    fun decompress(decode: ByteArray): String {
        if (decode.isEmpty()) {
            return ""
        }
        //创建一个新的解压缩器
        val inflater = Inflater()
        //设置解压缩的输入数据。
        inflater.setInput(decode)
        val bytes = ByteArray(2048)
        val outputStream = ByteArrayOutputStream(2048)
        try {
            //finished() 如果已到达压缩数据流的末尾，则返回true。
            while (!inflater.finished()) {
                //将字节解压缩到指定的缓冲区中。
                val length = inflater.inflate(bytes)
                outputStream.write(bytes, 0, length)
            }
        } catch (e: DataFormatException) {
            e.printStackTrace()
            return ""
        } finally {
            //关闭解压缩器并丢弃任何未处理的输入。
            inflater.end()
        }
        return outputStream.toString()
    }
}