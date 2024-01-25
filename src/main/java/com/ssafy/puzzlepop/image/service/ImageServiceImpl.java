package com.ssafy.puzzlepop.image.service;

import com.ssafy.puzzlepop.image.domain.Image;
import com.ssafy.puzzlepop.image.domain.ImageCreateDto;
import com.ssafy.puzzlepop.image.domain.ImageDto;
import com.ssafy.puzzlepop.image.domain.ImageResponseDto;
import com.ssafy.puzzlepop.image.exception.ImageException;
import com.ssafy.puzzlepop.image.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;

    @Autowired
    private ImageServiceImpl(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    private static final String RELATIVE_PATH = "./uploads/image/";

    ///////////

    @Override
    public int createImage(MultipartFile file, ImageCreateDto imageCreateDto) throws ImageException {

        // 권한 체크
        if ("sPuzzle".equals(imageCreateDto.getType()) || "item".equals(imageCreateDto.getType())) { //
            if (!"admin".equals(imageCreateDto.getUserId())) { // TODO: accessToken 발급받은 userId가 admin인지 확인
                throw new ImageException("업로드 권한이 없습니다.");
            }
        }

        // 이미지 저장
        Path absolutePath = Paths.get(RELATIVE_PATH).toAbsolutePath();
        String absolutePathString = absolutePath.toString();

        String originalFilename = file.getOriginalFilename(); // image.jpg
        String filename = originalFilename.substring(0, originalFilename.indexOf(".")); // image
        String name = UUID.randomUUID().toString() + "_" + filename; // uuid_image
        String filenameExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1); // jpg
        String filepath = Path.of(absolutePathString, name).toString(); // path/uuid_image

        // file의 확장자가 허용된 이미지 형식인지 확인 / 허용 확장자: png, jpeg, jpg, bmp
        if ("png".equals(filenameExtension) || "jpeg".equals(filenameExtension) || "jpg".equals(filenameExtension) || "bmp".equals(filenameExtension)) {
        } else { // not ok. return
            throw new ImageException("png, jpeg, jpg, bmp 형식의 파일만 업로드할 수 있습니다.");
        }

        // 파일 저장할 폴더 없다면 생성
        if (!absolutePath.toFile().exists()) {
            absolutePath.toFile().mkdirs();
        }

        // 파일 저장
        try {
            file.transferTo(new File(filepath + "." + filenameExtension));
        } catch (Exception e) {
            throw new ImageException("이미지 파일 업로드 중 에러 발생");
        }

        // image 객체에 file의 정보 뽑아서 담기
        Image image = new Image();

        image.setType(imageCreateDto.getType()); // 이미지타입
        image.setName(name); // 서버저장파일명
        image.setFilename(filename); // 원본파일명
        image.setFilepath(filepath); // 서버경로+서버저장파일명
        image.setFilenameExtension(filenameExtension); // 파일 확장자
        image.setUserId(imageCreateDto.getUserId()); // 업로드한 uid

        // 이미지 객체 db 저장
        try {
            imageRepository.save(image);
            return image.getId();
        } catch (Exception e) {
            throw new ImageException(e.getMessage());
        }
    }

    @Override
    public int updateImage(ImageDto imageDto) throws ImageException {
//        Image existImage = imageRepository.findById(imageDto.getId()).orElse(null);
//
//        if (existImage != null) {
//            existImage.setType(imageDto.getType());
//            existImage.setName(imageDto.getName());
//            existImage.setFilename(imageDto.getFilename());
//            existImage.setFilepath(imageDto.getFilepath());
//            existImage.setFilenameExtension(imageDto.getFilenameExtension());
//            existImage.setUserId(imageDto.getUserId());
//            existImage.setUpdateTime(new Date());
//
//            imageRepository.save(existImage);
//            return existImage.getId();
//        } else {
//            throw new ImageException("image to update doesn't exist");
//        }

        return 0;
    }

    @Override
    public void deleteImage(int id) throws ImageException {

        /*
        - 아이디에 해당하는 이미지정보가 db에 존재하는지 확인
        - a_t의 userId가 이미지정보의 user_id(업로더)와 일치하는지 확인
        - db에 저장된 filepath에 이미지 파일이 존재하는지 확인 & filepath에 저장된 이미지 파일 삭제
        - db에 저장된 이미지정보 데이터 삭제
         */

        // 1 존재하는 이미지인지 확인
        Image existImage = imageRepository.findById(id).orElse(null);

        if (existImage == null) { // 존재하지 않으면 exception throw
            throw new ImageException("image matches to id doesn't exist");
        }

        // 3 저장된 이미지 파일 삭제
        Path filepath = Path.of(existImage.getFilepath() + "." + existImage.getFilenameExtension());

        try {
            if (!Files.deleteIfExists(filepath)) {
                throw new ImageException("image file doesn't exist");
            }
        } catch (IOException e) {
            throw new ImageException("failed to delete image file");
        }

        // 4 db의 데이터 삭제
        try {
            imageRepository.deleteById(id);
        } catch (Exception e) {
            throw new ImageException("failed to delete imageinfo in db");
        }
    }

    @Override
    public ImageDto getImageById(int id) throws ImageException {
        return null;
    }

//    @Override
//    public ImageResponseDto getImageById(int id) throws ImageException {
//        Image image;
//
//        try {
//            image = imageRepository.findById(id).orElse(null);
//            if (image == null) { // 존재하는 id에 대한 요청만 허용한다고 가정. 필요 시 수정
//                throw new ImageException("존재하지 않는 이미지");
//            }
//
//            ImageResponseDto imageResponseDto = new ImageResponseDto();
//            imageResponseDto.setId(image.getId());
//            imageResponseDto.setFilename(image.getFilename());
//            Path imagePath = Path.of(image.getFilepath()+"."+image.getFilenameExtension());
//            UrlResource imageUrl = new UrlResource(imagePath.toUri());
//            imageResponseDto.setImageUrl(imageUrl);
//
//            return imageResponseDto;
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new ImageException(e.getMessage());
//        }
//    }

    @Override
    public List<ImageDto> getAllImages() throws ImageException {

        List<Image> imageList;

        try {
            imageList = imageRepository.findAll();
            return imageList.stream().map(ImageDto::new).collect(Collectors.toList());
        } catch (Exception e) {
            throw new ImageException("failed to get all images");
        }
    }

    @Override
    public List<ImageDto> getImagesByType(String type) throws ImageException {
        List<Image> imageList;

        try {
            imageList = imageRepository.findAllByType(type);
            return imageList.stream().map(ImageDto::new).collect(Collectors.toList());
        } catch (Exception e) {
            throw new ImageException("failed to get images by type");
        }
    }



}
