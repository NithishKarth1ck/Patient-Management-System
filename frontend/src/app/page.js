import styles from "./page.module.css";

export default function Home() {
  return (
  <div className={styles.container}> 
   <div>
    <h1 className={styles.title}>Patient Management Dashboard</h1>
    
    <p>Dashboard Overview</p>
   </div>

  <div className={styles.cards}>
   <div className={styles.card}>
     <h2>Total Patients</h2>
     <p>120</p>
   </div>

   <div className={styles.card}>
    <h2>New This Month</h2>
    <p>12</p>
   </div>
  </div>

   <table className={styles.table}>
    <thead>
      <tr>
        <th>Name</th>
        <th>Email</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>John Doe</td>
        <td>johndoe@email.com</td>
      </tr>
      <tr>
        <td>Jane Smith</td>
        <td>janesmith@email.com</td>
      </tr>
      <tr>
        <td>Alex Morgan</td>
        <td>alex.morgan@email.com</td>
      </tr>
    </tbody>
   </table>
  </div> 
  );
}

