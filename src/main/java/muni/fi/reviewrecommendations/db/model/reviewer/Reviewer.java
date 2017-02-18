package muni.fi.reviewrecommendations.db.model.reviewer;

import com.google.gerrit.extensions.common.AccountInfo;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class Reviewer {
    @Id
    private Integer id;

    private String email;
    private String name;
    private String avatar;

    public Reviewer() {
    }

    public Reviewer(AccountInfo accountInfo) {
        this.id = accountInfo._accountId;
        this.email = accountInfo.email;
        this.name = accountInfo.name;
        if (accountInfo.avatars != null && accountInfo.avatars.size() > 0) {
            this.avatar = accountInfo.avatars.get(0).url;
        }
    }

    public Reviewer(String email, String name) {
        this.email = email;
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reviewer reviewer = (Reviewer) o;

        if (!id.equals(reviewer.id)) return false;
        if (email != null ? !email.equals(reviewer.email) : reviewer.email != null) return false;
        return name != null ? name.equals(reviewer.name) : reviewer.name == null;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
