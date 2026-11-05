package vn.bookeasy.booking;

import vn.bookeasy.business.BusinessCategory;
import vn.bookeasy.business.PaymentMode;

import java.util.List;

public final class PublicDtos {
    private PublicDtos() {
    }

    public record PublicBranch(Long id, String name, String address, String phone) {
    }

    public record PublicService(Long id, String name, String description, int durationMinutes, long price,
                                Long categoryId) {
    }

    public record PublicCategory(Long id, String name) {
    }

    public record PublicStaff(Long id, String fullName, String title, String avatarUrl, String color) {
    }

    public record PublicBusiness(String slug, String name, String description, String phone, String address,
                                 String logoUrl, String brandColor, BusinessCategory category,
                                 PaymentMode paymentMode, int depositPercent, int bookingWindowDays,
                                 List<PublicBranch> branches, List<PublicCategory> categories,
                                 List<PublicService> services) {
    }
}
