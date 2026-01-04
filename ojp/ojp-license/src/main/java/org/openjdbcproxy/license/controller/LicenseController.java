package org.openjdbcproxy.license.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.openjdbcproxy.license.controller.dto.LicenseDTO;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * 商业授权管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/license")
@Tag(name = "License Management", description = "商业授权管理API")
public class LicenseController {

    // 支持开发环境和生产环境的路径配置
    // 开发环境(IDE/源码运行): 使用项目目录下的 ojp-data/license
    // 生产环境(jar包运行): 使用 Docker 挂载卷 /etc/ojp
    private static final String LICENSE_PATH = isRunningFromJar()
            ? "/etc/ojp/license/license.key"
            : "/home/slm/code/a8-turbo/docker/ojp/license/license.key";
    
    /**
     * 判断是否从jar包运行(生产环境)
     * @return true表示从jar包运行,false表示从IDE或源码运行
     */
    private static boolean isRunningFromJar() {
        try {
            String protocol = LicenseController.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getProtocol();
            return "jar".equals(protocol);
        } catch (Exception e) {
            log.warn("无法检测运行模式,默认使用开发环境路径", e);
            return false;
        }
    }

    static {
        log.info("License file path configured as: {}", LICENSE_PATH);
    }

    @GetMapping
    @Operation(summary = "获取当前授权信息", description = "从共享卷读取并解析授权 Payload")
    public LicenseDTO getLicenseInfo() {
        Path path = Paths.get(LICENSE_PATH);
        if (!Files.exists(path)) {
            return LicenseDTO.builder()
                    .valid(false)
                    .message("未检测到有效的授权文件")
                    .build();
        }

        try {
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
            return parseLicense(content);
        } catch (IOException e) {
            log.error("读取授权文件失败", e);
            return LicenseDTO.builder().valid(false).message("读取授权文件失败: " + e.getMessage()).build();
        }
    }

    @PostMapping
    @Operation(summary = "提交新授权码", description = "保存并持久化授权码到共享卷，触发哨兵更新")
    public LicenseDTO updateLicense(@RequestBody String licenseCode) {
        if (licenseCode == null || !licenseCode.contains(".")) {
             return LicenseDTO.builder().valid(false).message("无效的授权码格式 (需为 Payload.Signature)").build();
        }

        try {
            // 确保目录存在
            File file = new File(LICENSE_PATH);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Files.write(Paths.get(LICENSE_PATH), licenseCode.getBytes(StandardCharsets.UTF_8));
            log.info("成功更新授权文件: {}", LICENSE_PATH);
            
            // 立即解析并返回给前端展示
            LicenseDTO dto = parseLicense(licenseCode);
            dto.setMessage("授权码已成功提交并保存");
            return dto;
        } catch (IOException e) {
            log.error("写入授权文件失败", e);
            return LicenseDTO.builder().valid(false).message("服务器权限不足，写入失败: " + e.getMessage()).build();
        }
    }

    private LicenseDTO parseLicense(String code) {
        try {
            String[] parts = code.split("\\.");
            if (parts.length < 1) {
                return LicenseDTO.builder().valid(false).message("格式错误").build();
            }

            // 解码第一部分 (Payload)
            byte[] decodedBytes = Base64.getDecoder().decode(parts[0]);
            String payloadJson = new String(decodedBytes, StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(payloadJson);

            return LicenseDTO.builder()
                    .customer(json.getString("customer"))
                    .expiryDate(json.getString("expiry_date"))
                    .licenseCode(code)
                    .valid(true)
                    .build();
        } catch (Exception e) {
            log.warn("解析授权 Payload 失败: {}", e.getMessage());
            return LicenseDTO.builder()
                    .licenseCode(code)
                    .valid(false)
                    .message("Payload 解析失败，签名完整性由哨兵负责校验")
                    .build();
        }
    }
}
