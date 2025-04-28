package com.difegue.doujinsoft.utils;

import com.xperia64.diyedit.editors.GameEdit;
import com.xperia64.diyedit.editors.MangaEdit;
import com.xperia64.diyedit.metadata.Metadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Base64;
import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.*;

public class TestMioUtils {

    @Test
    public void testTypes() {
        Assertions.assertEquals(65536, MioUtils.Types.GAME);
        Assertions.assertEquals(14336, MioUtils.Types.MANGA);
        Assertions.assertEquals(8192, MioUtils.Types.RECORD);
        Assertions.assertEquals(255, MioUtils.Types.SURVEY);
    }

    @Test
    public void testGetTimeStringWithNegativeInteger() {
        Assertions.assertEquals("31/12/1999", MioUtils.getTimeString(-1));
    }

    @Test
    public void testGetTimeStringWithZeroInteger() {
        Assertions.assertEquals("01/01/2000", MioUtils.getTimeString(0));
    }

    @Test
    public void testGetTimeStringWithPositiveInteger() {
        Assertions.assertEquals("02/01/2000", MioUtils.getTimeString(1));
    }

    @Test
    public void testComputeMioID() {
        Metadata mio = mock(Metadata.class);
        InOrder inOrder = inOrder(mio);
        when(mio.getSerial1()).thenReturn("nint");
        when(mio.getSerial2()).thenReturn(10100);
        when(mio.getSerial3()).thenReturn(0);
        MioUtils.computeMioID(mio);
        inOrder.verify(mio, times(1)).getSerial1();
        inOrder.verify(mio, times(1)).getSerial2();
        inOrder.verify(mio, times(1)).getSerial3();
        inOrder.verifyNoMoreInteractions();
        Assertions.assertEquals("nint-10100-0", MioUtils.computeMioID(mio));
    }

    @ParameterizedTest
    @CsvSource({
            "0, yellow",
            "1, light-blue",
            "2, green",
            "3, orange",
            "4, indigo",
            "5, red",
            "6, grey lighten-3",
            "7, grey darken-4",
            "8, purple"
    })
    public void testMapColorByte(byte color, String colorString) {
        Assertions.assertEquals(colorString, MioUtils.mapColorByte(color));
    }

    @Test
    public void testGetBase64MangaWithAllBlack() {
        byte[] mioFile = new byte[]{};
        try (MockedConstruction<MangaEdit> mangaEdit = mockConstruction(MangaEdit.class,
                (e2, context) -> {
            when(e2.getPixel(anyByte(), anyInt(), anyInt())).thenReturn(true);
        })){
            MioUtils.getBase64Manga(mioFile, 0);
            List<MangaEdit> mangaEdits = mangaEdit.constructed();
            Assertions.assertEquals(1, mangaEdits.size());
            MangaEdit e2 = mangaEdits.get(0);
            verify(e2, times(191*127)).getPixel(anyByte(), anyInt(), anyInt());
        }
    }

    @Test
    public void testGetBase64MangaWithAllWhite() {
        byte[] mioFile = new byte[]{};
        try (MockedConstruction<MangaEdit> mangaEdit = mockConstruction(MangaEdit.class,
                (e2, context) -> {
                    when(e2.getPixel(anyByte(), anyInt(), anyInt())).thenReturn(false);
                })){
            MioUtils.getBase64Manga(mioFile, 0);
            List<MangaEdit> mangaEdits = mangaEdit.constructed();
            Assertions.assertEquals(1, mangaEdits.size());
            MangaEdit e2 = mangaEdits.get(0);
            verify(e2, times(191*127)).getPixel(anyByte(), anyInt(), anyInt());
        }
    }

    @Test
    public void testGetBase64MangaWithBlackAndWhite() {
        byte[] mioFile = new byte[]{};
        try (MockedConstruction<MangaEdit> mangaEdit = mockConstruction(MangaEdit.class,
                (e2, context) -> {
                    when(e2.getPixel(anyByte(), anyInt(), anyInt())).thenReturn(new Random().nextBoolean());
                })){
            MioUtils.getBase64Manga(mioFile, 0);
            List<MangaEdit> mangaEdits = mangaEdit.constructed();
            Assertions.assertEquals(1, mangaEdits.size());
            MangaEdit e2 = mangaEdits.get(0);
            verify(e2, times(191*127)).getPixel(anyByte(), anyInt(), anyInt());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14})
    public void testBase64GamePreview(int color) {
        byte[] mioFile = new byte[]{};
        try (MockedConstruction<GameEdit> gameEdit = mockConstruction(GameEdit.class,
                (gameMeta, context) -> {
                    when(gameMeta.getPreviewPixel(anyInt(), anyInt())).thenReturn(color);
                })){
            MioUtils.getBase64GamePreview(mioFile);
            List<GameEdit> gameEdits = gameEdit.constructed();
            Assertions.assertEquals(1, gameEdits.size());
            GameEdit gameMeta = gameEdits.get(0);
            verify(gameMeta, times(95*63)).getPreviewPixel(anyInt(), anyInt());
        }
    }

    @Test
    public void testImgToBase64StringWithNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {MioUtils.imgToBase64String(null, "png");});
    }

    @RepeatedTest(50)
    public void testImgToBase64StringWithOnePixelThenToImgToSeeIfTheSame() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, new Random().nextInt(256));
        String base64String = MioUtils.imgToBase64String(image, "png");

        byte[] decoded = Base64.getDecoder().decode(base64String);
        BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(decoded));

        Assertions.assertEquals(image.getWidth(), decodedImage.getWidth());
        Assertions.assertEquals(image.getHeight(), decodedImage.getHeight());
        Assertions.assertEquals(image.getRGB(0, 0), decodedImage.getRGB(0, 0));
    }

    @RepeatedTest(50)
    public void testImgToBase64StringWithMoreThanOnePixelThenToImgToSeeIfTheSame() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, new Random().nextInt(256));
        image.setRGB(1, 0, new Random().nextInt(256));
        image.setRGB(1, 1, new Random().nextInt(256));
        image.setRGB(0, 1, new Random().nextInt(256));
        String base64String = MioUtils.imgToBase64String(image, "png");

        byte[] decoded = Base64.getDecoder().decode(base64String);
        BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(decoded));

        Assertions.assertEquals(image.getWidth(), decodedImage.getWidth());
        Assertions.assertEquals(image.getHeight(), decodedImage.getHeight());
        Assertions.assertEquals(image.getRGB(0, 0), decodedImage.getRGB(0, 0));
        Assertions.assertEquals(image.getRGB(1, 0), decodedImage.getRGB(1, 0));
        Assertions.assertEquals(image.getRGB(1, 1), decodedImage.getRGB(1, 1));
        Assertions.assertEquals(image.getRGB(0, 1), decodedImage.getRGB(0, 1));
    }


    @Test
    public void testImgToBase64StringWithIOInterruptions() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, new Random().nextInt(256));
        try (MockedStatic<ImageIO> io = mockStatic(ImageIO.class)) {
            io.when(() -> ImageIO.write(any(RenderedImage.class), anyString(), any(OutputStream.class))).thenThrow(new IOException());
            Assertions.assertThrows(UncheckedIOException.class, () -> {MioUtils.imgToBase64String(image, "png");});
        }
    }

}
