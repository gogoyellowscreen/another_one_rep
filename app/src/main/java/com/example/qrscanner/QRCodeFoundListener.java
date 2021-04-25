package com.example.qrscanner;

import com.google.zxing.Result;
import com.google.zxing.client.result.ParsedResult;

public interface QRCodeFoundListener {
    void getParsedResult(ParsedResult result);
    void qrCodeNotFound();
}
