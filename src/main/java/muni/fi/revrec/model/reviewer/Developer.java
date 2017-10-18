package muni.fi.revrec.model.reviewer;

import com.google.gerrit.extensions.common.AccountInfo;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * @author Jakub Lipcak, Masaryk University
 */
@Entity
public class Developer {
    @Id
    private Integer id;

    private String accountId;
    private String email;
    private String name;
    private String avatar;

    public Developer() {
    }

    public Developer(AccountInfo accountInfo) {
        this.id = accountInfo._accountId;
        this.email = accountInfo.email;
        this.name = accountInfo.name;
        if (accountInfo.avatars != null && accountInfo.avatars.size() > 0) {
            this.avatar = accountInfo.avatars.get(0).url;
        }
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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Developer developer = (Developer) o;

        if (id != null ? !id.equals(developer.id) : developer.id != null) return false;
        if (accountId != null ? !accountId.equals(developer.accountId) : developer.accountId != null) return false;
        if (email != null ? !email.equals(developer.email) : developer.email != null) return false;
        if (name != null ? !name.equals(developer.name) : developer.name != null) return false;
        return avatar != null ? avatar.equals(developer.avatar) : developer.avatar == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (accountId != null ? accountId.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (avatar != null ? avatar.hashCode() : 0);
        return result;
    }
}