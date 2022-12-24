package com.udacity.catpoint.image.service;

import java.awt.image.BufferedImage;

public interface ImageService {
    public boolean imageContainsCat(BufferedImage currentCameraImage, float v);
}
